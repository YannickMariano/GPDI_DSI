package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.client.OllamaClient;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseSuggestionRisque;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseSuggestionRisque.SolutionProposee;
import mg.gov.mef.dsi.gpdi.dto.response.SolutionRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.PorteeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandation;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.exception.RisqueIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutRisqueInvalideException;
import mg.gov.mef.dsi.gpdi.exception.SuggestionDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.mapper.IaRecommandationMapper;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.ContexteProjetService;
import mg.gov.mef.dsi.gpdi.service.ReponseSuggestionRisqueService;
import mg.gov.mef.dsi.gpdi.service.SuggestionRisqueService;
import mg.gov.mef.dsi.gpdi.util.SuggestionRisqueConsignes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Comme la detection, l'appel au modele se fait hors transaction ; la reponse est ensuite enregistree
 * en une seule transaction (analyse + solutions), ou pas du tout. Les solutions en attente (PROPOSEE)
 * font office de cache : tant qu'elles existent pour un risque, le modele n'est pas re-interroge.
 */
@Service
@RequiredArgsConstructor
public class SuggestionRisqueServiceImpl implements SuggestionRisqueService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionRisqueServiceImpl.class);

    private final ContexteProjetService contexteProjetService;
    private final ReponseSuggestionRisqueService reponseSuggestionRisqueService;
    private final OllamaClient ollamaClient;
    private final RisqueRepository risqueRepository;
    private final IaAnalyseRepository iaAnalyseRepository;
    private final IaRecommandationRepository iaRecommandationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final IaRecommandationMapper iaRecommandationMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    /** Risques pour lesquels cette instance de l'application cherche des solutions en ce moment. */
    private final Set<Long> risquesEnRecherche = ConcurrentHashMap.newKeySet();

    @Override
    public List<SolutionRisqueResponse> suggerer(Long idProjet, Long idRisque, Long idDemandeur) {
        Risque risque = obtenirRisqueOuvert(idProjet, idRisque);

        List<IaRecommandation> enAttente = solutionsEnAttente(idRisque);
        if (!enAttente.isEmpty()) {
            return versReponses(enAttente);
        }

        if (!risquesEnRecherche.add(idRisque)) {
            throw new SuggestionDejaEnCoursException(idRisque);
        }
        try {
            // Une recherche terminee entre la premiere verification et la prise du verrou.
            enAttente = solutionsEnAttente(idRisque);
            if (!enAttente.isEmpty()) {
                return versReponses(enAttente);
            }

            ContexteProjetIa contexte = contexteProjetService.construire(idProjet);
            String reponseBrute = ollamaClient.genererJson(SuggestionRisqueConsignes.consigneSysteme(),
                    SuggestionRisqueConsignes.consigneUtilisateur(contexte.getContenu(), decrireRisque(risque)),
                    SuggestionRisqueConsignes.schemaReponse());
            ReponseSuggestionRisque reponse = reponseSuggestionRisqueService.interpreter(reponseBrute,
                    contexte.getIdsPorteursPossibles(), tachesReaffectables(contexte.getContenu()));

            List<IaRecommandation> creees = transactionTemplate.execute(
                    statut -> enregistrer(idProjet, idRisque, idDemandeur, contexte, reponseBrute, reponse));
            return versReponses(creees);
        } finally {
            risquesEnRecherche.remove(idRisque);
        }
    }

    private Risque obtenirRisqueOuvert(Long idProjet, Long idRisque) {
        Risque risque = risqueRepository.findById(idRisque)
                .filter(trouve -> trouve.getProjet().getId().equals(idProjet))
                .orElseThrow(() -> new RisqueIntrouvableException(idRisque));
        verifierOuvert(risque);
        return risque;
    }

    private void verifierOuvert(Risque risque) {
        if (risque.getStatut() != StatutRisque.OUVERT) {
            throw new StatutRisqueInvalideException(risque.getId(), risque.getStatut(), "recevoir de nouvelles solutions");
        }
    }

    private List<IaRecommandation> solutionsEnAttente(Long idRisque) {
        return iaRecommandationRepository.findByRisqueIdOrderByIdAsc(idRisque).stream()
                .filter(recommandation -> recommandation.getStatut() == StatutRecommandation.PROPOSEE)
                .collect(Collectors.toList());
    }

    /**
     * Le statut est relu ici : pendant l'appel au modele, le risque a pu etre rejete ou clos, et des
     * solutions ne doivent alors pas etre rattachees a un risque qui n'est plus a traiter.
     */
    private List<IaRecommandation> enregistrer(Long idProjet, Long idRisque, Long idDemandeur, ContexteProjetIa contexte,
                                               String reponseBrute, ReponseSuggestionRisque reponse) {
        Risque risque = obtenirRisqueOuvert(idProjet, idRisque);

        IaAnalyse analyse = new IaAnalyse();
        analyse.setProjet(risque.getProjet());
        analyse.setDemandeur(idDemandeur == null ? null : utilisateurRepository.findById(idDemandeur).orElse(null));
        analyse.setTypeAnalyse(TypeAnalyseIa.SUGGESTION_RISQUE);
        analyse.setPortee(PorteeAnalyseIa.PROJET);
        analyse.setQuestion("Solutions pour le risque « " + risque.getLibelle() + " »");
        analyse.setReponse(reponseBrute);
        analyse.setContexte(contexte.getContenu());
        analyse.setModele(ollamaClient.getModele());
        IaAnalyse analyseEnregistree = iaAnalyseRepository.save(analyse);

        List<IaRecommandation> creees = new ArrayList<>();
        for (SolutionProposee solution : reponse.getSolutions()) {
            IaRecommandation recommandation = new IaRecommandation();
            recommandation.setAnalyse(analyseEnregistree);
            recommandation.setRisque(risque);
            recommandation.setLibelle(solution.getLibelle());
            recommandation.setJustification(solution.getJustification());
            recommandation.setActionProposee(decrireAction(solution));
            creees.add(iaRecommandationRepository.save(recommandation));
        }
        log.info("Analyse {} : {} solution(s) proposee(s) pour le risque {}", analyseEnregistree.getId(), creees.size(), idRisque);
        return creees;
    }

    /**
     * Action conservee avec la solution, relue au moment de l'appliquer :
     * REASSIGNER_TACHE {idTache, idAssigne, gain} ou CREER_TACHE {titre, description, tempsEstime, idAssigne, gain}
     * (les champs facultatifs sont omis s'ils sont absents).
     */
    private JsonNode decrireAction(SolutionProposee solution) {
        ObjectNode action = objectMapper.createObjectNode();
        action.put("type", solution.getType().name());
        if (solution.getType() == TypeActionRecommandation.REASSIGNER_TACHE) {
            action.put("idTache", solution.getIdTache());
            action.put("idAssigne", solution.getIdAssigne());
        } else {
            action.put("titre", solution.getTitreTache());
            if (solution.getDescriptionTache() != null) {
                action.put("description", solution.getDescriptionTache());
            }
            if (solution.getTempsEstime() != null) {
                action.put("tempsEstime", solution.getTempsEstime());
            }
            if (solution.getIdAssigne() != null) {
                action.put("idAssigne", solution.getIdAssigne());
            }
        }
        if (solution.getGain() != null) {
            action.put("gain", solution.getGain());
        }
        return action;
    }

    private JsonNode decrireRisque(Risque risque) {
        ObjectNode noeud = objectMapper.createObjectNode();
        noeud.put("id", risque.getId());
        noeud.put("libelle", risque.getLibelle());
        if (risque.getDescription() != null) {
            noeud.put("description", risque.getDescription());
        }
        noeud.put("probabilite", risque.getProbabilite());
        noeud.put("impact", risque.getImpact());
        noeud.put("niveau", String.valueOf(risque.getNiveau()));
        if (risque.getMesureMitigation() != null) {
            noeud.put("parade", risque.getMesureMitigation());
        }
        return noeud;
    }

    /** Seules les taches que le modele a vues (et qui sont ouvertes, le contexte n'incluant que celles-la) peuvent etre reaffectees. */
    private Set<Long> tachesReaffectables(JsonNode contexteProjet) {
        Set<Long> ids = new HashSet<>();
        for (JsonNode tache : contexteProjet.path("taches").path("aSurveiller")) {
            if (tache.path("id").isIntegralNumber()) {
                ids.add(tache.path("id").longValue());
            }
        }
        return ids;
    }

    private List<SolutionRisqueResponse> versReponses(List<IaRecommandation> recommandations) {
        return recommandations.stream().map(iaRecommandationMapper::toSolutionResponse).collect(Collectors.toList());
    }
}
