package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.client.ModeleIaClient;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque.ReevaluationRisque;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque.RisqueDetecte;
import mg.gov.mef.dsi.gpdi.dto.response.ResultatDetectionRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.PorteeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AnalyseDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.ContexteProjetService;
import mg.gov.mef.dsi.gpdi.service.DetectionRisqueService;
import mg.gov.mef.dsi.gpdi.service.ReponseDetectionRisqueService;
import mg.gov.mef.dsi.gpdi.util.DetectionRisqueConsignes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * L'appel au modele dure de quelques secondes a plusieurs minutes : il ne doit pas se faire dans une
 * transaction (elle garderait une connexion a la base bloquee). Le contexte est donc lu, le modele
 * interroge, puis tout ce qui en decoule est ecrit en une seule transaction courte : soit l'analyse
 * est enregistree en entier, soit rien ne l'est.
 */
@Service
@RequiredArgsConstructor
public class DetectionRisqueServiceImpl implements DetectionRisqueService {

    private static final Logger log = LoggerFactory.getLogger(DetectionRisqueServiceImpl.class);

    private final ContexteProjetService contexteProjetService;
    private final ReponseDetectionRisqueService reponseDetectionRisqueService;
    private final ModeleIaClient ollamaClient;
    private final ProjetRepository projetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RisqueRepository risqueRepository;
    private final IaAnalyseRepository iaAnalyseRepository;
    private final TransactionTemplate transactionTemplate;

    /** Projets en cours d'analyse par cette instance de l'application. */
    private final Set<Long> projetsEnAnalyse = ConcurrentHashMap.newKeySet();

    @Override
    public ResultatDetectionRisqueResponse detecter(Long idProjet, Long idDemandeur) {
        if (!projetRepository.existsById(idProjet)) {
            throw new ProjetIntrouvableException(idProjet);
        }
        if (!projetsEnAnalyse.add(idProjet)) {
            throw new AnalyseDejaEnCoursException(idProjet);
        }
        try {
            ContexteProjetIa contexte = contexteProjetService.construire(idProjet);
            String reponseBrute = ollamaClient.genererJson(DetectionRisqueConsignes.consigneSysteme(),
                    DetectionRisqueConsignes.consigneUtilisateur(contexte.getContenu()),
                    DetectionRisqueConsignes.schemaReponse());
            ReponseDetectionRisque reponse = reponseDetectionRisqueService.interpreter(reponseBrute, contexte);
            return transactionTemplate.execute(statut -> enregistrer(idProjet, idDemandeur, contexte, reponse));
        } finally {
            projetsEnAnalyse.remove(idProjet);
        }
    }

    private ResultatDetectionRisqueResponse enregistrer(Long idProjet, Long idDemandeur, ContexteProjetIa contexte,
                                                        ReponseDetectionRisque reponse) {
        Projet projet = projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));

        IaAnalyse analyse = new IaAnalyse();
        analyse.setProjet(projet);
        analyse.setDemandeur(idDemandeur == null ? null : utilisateurRepository.findById(idDemandeur).orElse(null));
        analyse.setTypeAnalyse(TypeAnalyseIa.DETECTION_RISQUE);
        analyse.setPortee(PorteeAnalyseIa.PROJET);
        analyse.setReponse(reponse.getSynthese());
        analyse.setContexte(contexte.getContenu());
        analyse.setIndiceConfiance(reponse.getIndiceConfiance());
        analyse.setModele(ollamaClient.getModele());
        IaAnalyse analyseEnregistree = iaAnalyseRepository.save(analyse);

        int[] compteurs = reevaluer(idProjet, reponse);
        int nombreNouveaux = creer(projet, analyseEnregistree, reponse);

        log.info("Analyse {} du projet {} : {} nouveau(x) risque(s), {} reevalue(s), {} clos",
                analyseEnregistree.getId(), idProjet, nombreNouveaux, compteurs[0], compteurs[1]);
        return new ResultatDetectionRisqueResponse(analyseEnregistree.getId(), nombreNouveaux,
                compteurs[0], compteurs[1], reponse.getSynthese());
    }

    /**
     * Le statut est relu ici : pendant l'appel au modele, un risque a pu etre rejete ou clos, et il ne
     * faut alors pas le modifier. Renvoie {nombre reevalue, nombre clos}.
     */
    private int[] reevaluer(Long idProjet, ReponseDetectionRisque reponse) {
        Map<Long, Risque> risquesDuProjet = new HashMap<>();
        for (Risque risque : risqueRepository.findByProjetIdOrderByIdAsc(idProjet)) {
            risquesDuProjet.put(risque.getId(), risque);
        }

        int reevalues = 0;
        int clos = 0;
        for (ReevaluationRisque reevaluation : reponse.getReevaluations()) {
            Risque risque = risquesDuProjet.get(reevaluation.getIdRisque());
            if (risque == null || risque.getStatut() != StatutRisque.SOUS_CONTROLE) {
                continue;
            }
            risque.setProbabilite(reevaluation.getProbabilite());
            risque.setImpact(reevaluation.getImpact());
            risque.setNiveau(NiveauRisque.calculer(reevaluation.getProbabilite(), reevaluation.getImpact()));
            if (risque.getNiveau() == NiveauRisque.FAIBLE) {
                risque.setStatut(StatutRisque.CLOS);
                clos++;
            }
            risqueRepository.save(risque);
            reevalues++;
        }
        return new int[]{reevalues, clos};
    }

    private int creer(Projet projet, IaAnalyse analyse, ReponseDetectionRisque reponse) {
        Map<Long, Utilisateur> porteurs = new HashMap<>();
        for (RisqueDetecte detecte : reponse.getNouveauxRisques()) {
            Risque risque = new Risque();
            risque.setProjet(projet);
            risque.setAnalyse(analyse);
            risque.setPorteur(porteurPour(detecte.getIdPorteur(), projet, porteurs));
            risque.setLibelle(detecte.getLibelle());
            risque.setDescription(detecte.getDescription());
            risque.setProbabilite(detecte.getProbabilite());
            risque.setImpact(detecte.getImpact());
            risque.setNiveau(NiveauRisque.calculer(detecte.getProbabilite(), detecte.getImpact()));
            risque.setStatut(StatutRisque.OUVERT);
            risque.setMesureMitigation(detecte.getParade());
            risqueRepository.save(risque);
        }
        return reponse.getNouveauxRisques().size();
    }

    /** Porteur propose par l'IA (membre de l'equipe, deja verifie), sinon le responsable du projet. */
    private Utilisateur porteurPour(Long idPorteur, Projet projet, Map<Long, Utilisateur> dejaCharges) {
        if (idPorteur == null) {
            return projet.getResponsable();
        }
        Utilisateur porteur = dejaCharges.get(idPorteur);
        if (porteur == null) {
            porteur = utilisateurRepository.findById(idPorteur).orElse(projet.getResponsable());
            dejaCharges.put(idPorteur, porteur);
        }
        return porteur;
    }
}
