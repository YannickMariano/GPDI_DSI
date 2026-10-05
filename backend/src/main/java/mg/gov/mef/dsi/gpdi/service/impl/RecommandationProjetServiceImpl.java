package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.client.OllamaClient;
import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.RecommandationProjetResponse;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseRecommandationProjet;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseRecommandationProjet.RecommandationProposee;
import mg.gov.mef.dsi.gpdi.dto.response.TacheResponse;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.PorteeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandationProjet;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesRecommandationsProjetRefuseException;
import mg.gov.mef.dsi.gpdi.exception.AnalyseRecommandationsDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.RecommandationProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import mg.gov.mef.dsi.gpdi.exception.StatutRecommandationProjetInvalideException;
import mg.gov.mef.dsi.gpdi.exception.TacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurNonMembreEquipeException;
import mg.gov.mef.dsi.gpdi.mapper.RecommandationProjetMapper;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.ContexteProjetService;
import mg.gov.mef.dsi.gpdi.service.RecommandationProjetService;
import mg.gov.mef.dsi.gpdi.service.ReponseRecommandationProjetService;
import mg.gov.mef.dsi.gpdi.service.TacheService;
import mg.gov.mef.dsi.gpdi.util.RecommandationProjetConsignes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Comme pour les risques, l'appel au modele se fait hors transaction ; les recommandations sont
 * ensuite enregistrees en une seule transaction, ou pas du tout. L'application d'une recommandation
 * est independante des autres (contrairement aux solutions de risque, il n'y a pas de "tout ou rien").
 */
@Service
@RequiredArgsConstructor
public class RecommandationProjetServiceImpl implements RecommandationProjetService {

    private static final Logger log = LoggerFactory.getLogger(RecommandationProjetServiceImpl.class);
    private static final Set<StatutTache> STATUTS_TACHE_CLOS = EnumSet.of(StatutTache.TERMINEE, StatutTache.REPORTEE);
    /** Seul ce rôle peut, en plus du chef de projet, consulter (lecture seule) les recommandations de n'importe quel projet du portefeuille. */
    private static final String ROLE_DIRECTEUR_DSI = "directeur_dsi";

    private final ProjetRepository projetRepository;
    private final IaAnalyseRepository iaAnalyseRepository;
    private final IaRecommandationRepository iaRecommandationRepository;
    private final TacheRepository tacheRepository;
    private final AffectationTacheRepository affectationTacheRepository;
    private final MembreEquipeRepository membreEquipeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurCourantService utilisateurCourantService;
    private final ContexteProjetService contexteProjetService;
    private final ReponseRecommandationProjetService reponseRecommandationProjetService;
    private final RecommandationProjetMapper recommandationProjetMapper;
    private final TacheService tacheService;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    /** Projets pour lesquels cette instance de l'application analyse des recommandations en ce moment. */
    private final Set<Long> projetsEnAnalyse = ConcurrentHashMap.newKeySet();

    @Override
    @Transactional(readOnly = true)
    public List<RecommandationProjetResponse> lister(Long idProjet) {
        Projet projet = obtenirProjetAccessibleEnLecture(idProjet);
        return iaRecommandationRepository.findByAnalyseProjetIdAndRisqueIsNullAndStatutOrderByIdDesc(projet.getId(), StatutRecommandation.PROPOSEE)
                .stream()
                .map(recommandationProjetMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecommandationProjetResponse> analyser(Long idProjet, Long idDemandeur) {
        obtenirProjetAccessible(idProjet);
        if (!projetsEnAnalyse.add(idProjet)) {
            throw new AnalyseRecommandationsDejaEnCoursException(idProjet);
        }
        try {
            ContexteProjetIa contexte = contexteProjetService.construire(idProjet);
            List<String> dejaProposees = iaRecommandationRepository
                    .findByAnalyseProjetIdAndRisqueIsNullAndStatutOrderByIdDesc(idProjet, StatutRecommandation.PROPOSEE)
                    .stream().map(IaRecommandation::getLibelle).collect(Collectors.toList());

            String reponseBrute = ollamaClient.genererJson(RecommandationProjetConsignes.consigneSysteme(),
                    RecommandationProjetConsignes.consigneUtilisateur(contexte.getContenu(), dejaProposees),
                    RecommandationProjetConsignes.schemaReponse());
            ReponseRecommandationProjet reponse = reponseRecommandationProjetService.interpreter(
                    reponseBrute, idsTachesASurveiller(contexte.getContenu()), contexte.getIdsPorteursPossibles(), dejaProposees);

            return transactionTemplate.execute(statut -> enregistrer(idProjet, idDemandeur, contexte, reponse));
        } finally {
            projetsEnAnalyse.remove(idProjet);
        }
    }

    private List<RecommandationProjetResponse> enregistrer(Long idProjet, Long idDemandeur, ContexteProjetIa contexte,
                                                            ReponseRecommandationProjet reponse) {
        Projet projet = projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));

        IaAnalyse analyse = new IaAnalyse();
        analyse.setProjet(projet);
        analyse.setDemandeur(idDemandeur == null ? null : utilisateurRepository.findById(idDemandeur).orElse(null));
        analyse.setTypeAnalyse(TypeAnalyseIa.SYNTHESE);
        analyse.setPortee(PorteeAnalyseIa.PROJET);
        analyse.setContexte(contexte.getContenu());
        analyse.setModele(ollamaClient.getModele());
        IaAnalyse analyseEnregistree = iaAnalyseRepository.save(analyse);

        List<RecommandationProjetResponse> creees = new ArrayList<>();
        for (RecommandationProposee proposee : reponse.getRecommandations()) {
            IaRecommandation recommandation = new IaRecommandation();
            recommandation.setAnalyse(analyseEnregistree);
            recommandation.setLibelle(proposee.getLibelle());
            recommandation.setJustification(proposee.getJustification());
            recommandation.setActionProposee(decrireAction(proposee));
            creees.add(recommandationProjetMapper.toResponse(iaRecommandationRepository.save(recommandation)));
        }
        log.info("Analyse {} du projet {} : {} recommandation(s) proposee(s)", analyseEnregistree.getId(), idProjet, creees.size());
        return creees;
    }

    @Override
    @Transactional
    public RecommandationProjetResponse appliquer(Long idProjet, Long idRecommandation, Long idValidateur) {
        IaRecommandation recommandation = obtenirRecommandationOuverte(idProjet, idRecommandation);
        Utilisateur validateur = obtenirUtilisateur(idValidateur);
        Projet projet = recommandation.getAnalyse().getProjet();

        JsonNode action = recommandation.getActionProposee();
        String type = action.path("type").asText();
        String resultat;
        if (TypeActionRecommandationProjet.REASSIGNER_TACHE.name().equals(type)) {
            resultat = reaffecter(projet, action);
        } else if (TypeActionRecommandationProjet.RETIRER_DU_SPRINT.name().equals(type)) {
            resultat = retirerDuSprint(projet, action);
        } else if (TypeActionRecommandationProjet.AJUSTER_ESTIMATION.name().equals(type)) {
            resultat = ajusterEstimation(projet, action);
        } else {
            throw new ReponseIaInvalideException("Type d'action de recommandation inconnu : " + type + ".");
        }

        recommandation.setStatut(StatutRecommandation.APPLIQUEE);
        recommandation.setValidateur(validateur);
        recommandation.setDateValidation(LocalDateTime.now());
        recommandation.setResultatApplication(decrireResultat(resultat));
        IaRecommandation enregistree = iaRecommandationRepository.save(recommandation);
        log.info("Recommandation {} appliquee sur le projet {} par l'utilisateur {}", idRecommandation, idProjet, idValidateur);
        return recommandationProjetMapper.toResponse(enregistree);
    }

    @Override
    @Transactional
    public RecommandationProjetResponse rejeter(Long idProjet, Long idRecommandation, Long idValidateur) {
        IaRecommandation recommandation = obtenirRecommandationOuverte(idProjet, idRecommandation);
        Utilisateur validateur = obtenirUtilisateur(idValidateur);

        recommandation.setStatut(StatutRecommandation.REJETEE);
        recommandation.setValidateur(validateur);
        recommandation.setDateValidation(LocalDateTime.now());
        IaRecommandation enregistree = iaRecommandationRepository.save(recommandation);
        log.info("Recommandation {} rejetee sur le projet {} par l'utilisateur {}", idRecommandation, idProjet, idValidateur);
        return recommandationProjetMapper.toResponse(enregistree);
    }

    // --- Exécution des actions ---

    private String reaffecter(Projet projet, JsonNode action) {
        Long idAssigne = action.path("idAssigne").asLong();
        if (!estMembreDeLEquipe(projet, idAssigne)) {
            throw new UtilisateurNonMembreEquipeException(idAssigne);
        }
        Tache tache = tacheOuverteDuProjet(projet, action.path("idTache").asLong());
        TacheRequest requete = requeteDepuis(tache);
        requete.setIdsUtilisateursAssignes(Collections.singletonList(idAssigne));
        TacheResponse reponse = tacheService.modifier(projet.getId(), tache.getId(), requete);
        return "Tâche " + reponse.getCode() + " réaffectée.";
    }

    private String retirerDuSprint(Projet projet, JsonNode action) {
        Tache tache = tacheOuverteDuProjet(projet, action.path("idTache").asLong());
        TacheRequest requete = requeteDepuis(tache);
        requete.setIdSprint(null);
        TacheResponse reponse = tacheService.modifier(projet.getId(), tache.getId(), requete);
        return "Tâche " + reponse.getCode() + " retirée du sprint, renvoyée au backlog.";
    }

    private String ajusterEstimation(Projet projet, JsonNode action) {
        double nouvelleEstimation = action.path("nouvelleEstimation").asDouble();
        Tache tache = tacheOuverteDuProjet(projet, action.path("idTache").asLong());
        TacheRequest requete = requeteDepuis(tache);
        requete.setTempsEstime(nouvelleEstimation);
        TacheResponse reponse = tacheService.modifier(projet.getId(), tache.getId(), requete);
        return "Estimation de la tâche " + reponse.getCode() + " ajustée à " + nouvelleEstimation + " h.";
    }

    private Tache tacheOuverteDuProjet(Projet projet, Long idTache) {
        return tacheRepository.findById(idTache)
                .filter(tache -> tache.getProjet() != null && tache.getProjet().getId().equals(projet.getId()))
                .filter(tache -> !STATUTS_TACHE_CLOS.contains(tache.getStatut()))
                .orElseThrow(() -> new TacheIntrouvableException(idTache));
    }

    /**
     * Le TacheService remplace l'ensemble des champs modifiables : on lui redonne les valeurs actuelles
     * de la tache (y compris ses assignations), seul le champ vise par la recommandation change.
     */
    private TacheRequest requeteDepuis(Tache tache) {
        TacheRequest requete = new TacheRequest();
        requete.setTitre(tache.getTitre());
        requete.setDescription(tache.getDescription());
        requete.setPriorite(tache.getPriorite() != null ? tache.getPriorite() : Priorite.NORMALE);
        requete.setTempsEstime(tache.getTempsEstime());
        requete.setIdSprint(tache.getSprint() != null ? tache.getSprint().getId() : null);
        requete.setDateEcheance(tache.getDateFinPrevue());
        requete.setIdsUtilisateursAssignes(affectationTacheRepository.findByTacheId(tache.getId()).stream()
                .map(affectation -> affectation.getUtilisateur().getId())
                .collect(Collectors.toList()));
        return requete;
    }

    private boolean estMembreDeLEquipe(Projet projet, Long idUtilisateur) {
        return projet.getEquipe() != null
                && membreEquipeRepository.existsByEquipeIdAndUtilisateurId(projet.getEquipe().getId(), idUtilisateur);
    }

    // --- Utilitaires ---

    private JsonNode decrireAction(RecommandationProposee proposee) {
        ObjectNode action = objectMapper.createObjectNode();
        action.put("type", proposee.getType().name());
        action.put("idTache", proposee.getIdTache());
        if (proposee.getIdAssigne() != null) {
            action.put("idAssigne", proposee.getIdAssigne());
        }
        if (proposee.getNouvelleEstimation() != null) {
            action.put("nouvelleEstimation", proposee.getNouvelleEstimation());
        }
        if (proposee.getPreuve() != null) {
            action.put("preuve", proposee.getPreuve());
        }
        return action;
    }

    private JsonNode decrireResultat(String resultat) {
        ObjectNode noeud = objectMapper.createObjectNode();
        noeud.put("resultat", resultat);
        return noeud;
    }

    /** Seules les taches que le modele a vues (le contexte n'incluant que celles-la) peuvent etre visees. */
    private Set<Long> idsTachesASurveiller(JsonNode contexteProjet) {
        Set<Long> ids = new HashSet<>();
        for (JsonNode tache : contexteProjet.path("taches").path("aSurveiller")) {
            if (tache.path("id").isIntegralNumber()) {
                ids.add(tache.path("id").longValue());
            }
        }
        return ids;
    }

    private IaRecommandation obtenirRecommandationOuverte(Long idProjet, Long idRecommandation) {
        obtenirProjetAccessible(idProjet);
        IaRecommandation recommandation = iaRecommandationRepository.findById(idRecommandation)
                .filter(r -> r.getRisque() == null)
                .filter(r -> r.getAnalyse().getProjet().getId().equals(idProjet))
                .orElseThrow(() -> new RecommandationProjetIntrouvableException(idRecommandation, idProjet));
        if (recommandation.getStatut() != StatutRecommandation.PROPOSEE) {
            throw new StatutRecommandationProjetInvalideException(idRecommandation, recommandation.getStatut());
        }
        return recommandation;
    }

    private Utilisateur obtenirUtilisateur(Long idUtilisateur) {
        return utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new UtilisateurIntrouvableException(idUtilisateur));
    }

    /** Seul le chef de projet (responsable) du projet peut generer, appliquer ou rejeter ses recommandations de pilotage. */
    private Projet obtenirProjetAccessible(Long idProjet) {
        Projet projet = projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));
        Utilisateur utilisateurCourant = utilisateurCourantService.obtenirUtilisateurCourant();
        if (!estResponsable(projet, utilisateurCourant)) {
            throw new AccesRecommandationsProjetRefuseException(idProjet);
        }
        return projet;
    }

    /** Lecture seule : en plus du chef de projet, le Directeur DSI peut consulter les recommandations de n'importe quel projet du portefeuille. */
    private Projet obtenirProjetAccessibleEnLecture(Long idProjet) {
        Projet projet = projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));
        Utilisateur utilisateurCourant = utilisateurCourantService.obtenirUtilisateurCourant();
        boolean estDirecteurDsi = utilisateurCourant.getRole() != null && ROLE_DIRECTEUR_DSI.equals(utilisateurCourant.getRole().getNom());
        if (!estResponsable(projet, utilisateurCourant) && !estDirecteurDsi) {
            throw new AccesRecommandationsProjetRefuseException(idProjet);
        }
        return projet;
    }

    private boolean estResponsable(Projet projet, Utilisateur utilisateur) {
        return projet.getResponsable() != null && projet.getResponsable().getId().equals(utilisateur.getId());
    }
}
