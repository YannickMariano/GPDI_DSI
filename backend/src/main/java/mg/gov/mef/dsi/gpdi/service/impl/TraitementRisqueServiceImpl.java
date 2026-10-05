package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.SolutionTraiteeResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TacheResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TraitementRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandation;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.RecommandationsConflictuellesException;
import mg.gov.mef.dsi.gpdi.exception.RisqueIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.SolutionRisqueIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutRecommandationInvalideException;
import mg.gov.mef.dsi.gpdi.exception.StatutRisqueInvalideException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurIntrouvableException;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.TacheService;
import mg.gov.mef.dsi.gpdi.service.TraitementRisqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tout se passe dans une seule transaction : le TacheService existant y participe, donc si une solution
 * echoue, les taches deja creees ou reaffectees, les statuts et le risque sont annules ensemble.
 * Pour cette raison, aucune exception n'est rattrapee en cours de route : ce qui peut etre verifie
 * a l'avance (tache cloturee, membre hors equipe) l'est avant l'appel, et se resout en tache de suivi.
 */
@Service
@RequiredArgsConstructor
public class TraitementRisqueServiceImpl implements TraitementRisqueService {

    private static final Logger log = LoggerFactory.getLogger(TraitementRisqueServiceImpl.class);

    private static final int MAX_LONGUEUR_TITRE_TACHE = 200;
    private static final int MAX_LONGUEUR_DESCRIPTION_TACHE = 2000;
    private static final Set<StatutTache> STATUTS_TACHE_CLOS = EnumSet.of(StatutTache.TERMINEE, StatutTache.REPORTEE);

    private static final String MOTIF_NON_RETENUE = "Solution non retenue lors de l'application des solutions.";
    private static final String REMARQUE_REAFFECTATION_IMPOSSIBLE =
            "Réaffectation impossible (tâche introuvable, clôturée, ou membre hors de l'équipe du projet) : une tâche de suivi a été créée à la place.";
    private static final String REMARQUE_MEMBRE_HORS_EQUIPE =
            "Le membre proposé n'appartient pas à l'équipe du projet : la tâche a été créée sans assignation.";

    private final RisqueRepository risqueRepository;
    private final IaRecommandationRepository iaRecommandationRepository;
    private final TacheRepository tacheRepository;
    private final MembreEquipeRepository membreEquipeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TacheService tacheService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TraitementRisqueResponse appliquer(Long idProjet, Long idRisque, List<Long> idsSolutions, Long idValidateur) {
        Risque risque = obtenirRisqueOuvert(idProjet, idRisque, "recevoir l'application de solutions");
        Utilisateur validateur = obtenirUtilisateur(idValidateur);

        Map<Long, IaRecommandation> solutions = new HashMap<>();
        for (IaRecommandation solution : iaRecommandationRepository.findByRisqueIdOrderByIdAsc(idRisque)) {
            solutions.put(solution.getId(), solution);
        }
        // Tri par identifiant : ordre d'execution deterministe, quel que soit l'ordre des cases cochees.
        Set<Long> idsChoisis = new TreeSet<>(idsSolutions);
        for (Long idSolution : idsChoisis) {
            IaRecommandation solution = solutions.get(idSolution);
            if (solution == null) {
                throw new SolutionRisqueIntrouvableException(idSolution, idRisque);
            }
            if (solution.getStatut() != StatutRecommandation.PROPOSEE) {
                throw new StatutRecommandationInvalideException(idSolution, solution.getStatut());
            }
        }
        verifierAbsenceDeConflit(idsChoisis, solutions);

        LocalDateTime maintenant = LocalDateTime.now();
        List<SolutionTraiteeResponse> traitees = new ArrayList<>();
        for (Long idSolution : idsChoisis) {
            IaRecommandation solution = solutions.get(idSolution);
            SolutionTraiteeResponse traitee = executer(solution, risque);
            solution.setStatut(StatutRecommandation.APPLIQUEE);
            solution.setValidateur(validateur);
            solution.setDateValidation(maintenant);
            solution.setResultatApplication(decrireResultat(traitee));
            iaRecommandationRepository.save(solution);
            traitees.add(traitee);
        }

        for (IaRecommandation solution : solutions.values()) {
            if (solution.getStatut() == StatutRecommandation.PROPOSEE) {
                rejeterSolution(solution, validateur, maintenant, MOTIF_NON_RETENUE);
            }
        }

        risque.setStatut(StatutRisque.SOUS_CONTROLE);
        risqueRepository.save(risque);
        log.info("Risque {} : {} solution(s) appliquee(s) par l'utilisateur {}", idRisque, traitees.size(), idValidateur);
        return new TraitementRisqueResponse(idRisque, risque.getStatut().name(), traitees);
    }

    @Override
    @Transactional
    public TraitementRisqueResponse rejeter(Long idProjet, Long idRisque, String motif, Long idValidateur) {
        Risque risque = obtenirRisqueOuvert(idProjet, idRisque, "etre rejete");
        Utilisateur validateur = obtenirUtilisateur(idValidateur);
        String motifNettoye = motif == null || motif.trim().isEmpty() ? null : motif.trim();

        LocalDateTime maintenant = LocalDateTime.now();
        for (IaRecommandation solution : iaRecommandationRepository.findByRisqueIdOrderByIdAsc(idRisque)) {
            if (solution.getStatut() == StatutRecommandation.PROPOSEE) {
                rejeterSolution(solution, validateur, maintenant, motifNettoye);
            }
        }

        risque.setStatut(StatutRisque.REJETE);
        risqueRepository.save(risque);
        log.info("Risque {} rejete par l'utilisateur {}", idRisque, idValidateur);
        return new TraitementRisqueResponse(idRisque, risque.getStatut().name(), Collections.<SolutionTraiteeResponse>emptyList());
    }

    /**
     * Deux solutions cochees ne peuvent pas reaffecter la meme tache : l'une ecraserait silencieusement
     * l'autre. On bloque tout le lot plutot que de laisser la derniere executee gagner sans le dire.
     */
    private void verifierAbsenceDeConflit(Set<Long> idsChoisis, Map<Long, IaRecommandation> solutions) {
        Map<Long, Long> premiereSolutionParTache = new HashMap<>();
        for (Long idSolution : idsChoisis) {
            JsonNode action = solutions.get(idSolution).getActionProposee();
            if (!TypeActionRecommandation.REASSIGNER_TACHE.name().equals(action.path("type").asText())) {
                continue;
            }
            Long idTache = identifiant(action.path("idTache"));
            if (idTache == null) {
                continue;
            }
            Long premiere = premiereSolutionParTache.putIfAbsent(idTache, idSolution);
            if (premiere != null) {
                throw new RecommandationsConflictuellesException(codeTache(idTache));
            }
        }
    }

    private String codeTache(Long idTache) {
        return tacheRepository.findById(idTache).map(Tache::getCode).orElse(String.valueOf(idTache));
    }

    private Risque obtenirRisqueOuvert(Long idProjet, Long idRisque, String actionDemandee) {
        Risque risque = risqueRepository.findByIdForUpdate(idRisque)
                .filter(trouve -> trouve.getProjet().getId().equals(idProjet))
                .orElseThrow(() -> new RisqueIntrouvableException(idRisque));
        if (risque.getStatut() != StatutRisque.OUVERT) {
            throw new StatutRisqueInvalideException(idRisque, risque.getStatut(), actionDemandee);
        }
        return risque;
    }

    private Utilisateur obtenirUtilisateur(Long idUtilisateur) {
        return utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new UtilisateurIntrouvableException(idUtilisateur));
    }

    private void rejeterSolution(IaRecommandation solution, Utilisateur validateur, LocalDateTime date, String motif) {
        solution.setStatut(StatutRecommandation.REJETEE);
        solution.setValidateur(validateur);
        solution.setDateValidation(date);
        solution.setMotifRejet(motif);
        iaRecommandationRepository.save(solution);
    }

    private SolutionTraiteeResponse executer(IaRecommandation solution, Risque risque) {
        JsonNode action = solution.getActionProposee();
        if (TypeActionRecommandation.REASSIGNER_TACHE.name().equals(action.path("type").asText())) {
            SolutionTraiteeResponse reaffectation = reaffecter(solution, risque, action);
            if (reaffectation != null) {
                return reaffectation;
            }
            return creerTache(solution, risque, action, true, REMARQUE_REAFFECTATION_IMPOSSIBLE);
        }
        return creerTache(solution, risque, action, false, null);
    }

    /** Renvoie null si la reaffectation n'est pas realisable (a verifier ici, l'echec du TacheService annulerait tout). */
    private SolutionTraiteeResponse reaffecter(IaRecommandation solution, Risque risque, JsonNode action) {
        Projet projet = risque.getProjet();
        Long idTache = identifiant(action.path("idTache"));
        Long idAssigne = identifiant(action.path("idAssigne"));
        if (idTache == null || idAssigne == null || !estMembreDeLEquipe(projet, idAssigne)) {
            return null;
        }
        Tache tache = tacheRepository.findById(idTache)
                .filter(trouvee -> trouvee.getProjet() != null && trouvee.getProjet().getId().equals(projet.getId()))
                .filter(trouvee -> !STATUTS_TACHE_CLOS.contains(trouvee.getStatut()))
                .orElse(null);
        if (tache == null) {
            return null;
        }

        // Le TacheService remplace l'ensemble des champs modifiables : on lui redonne les valeurs actuelles
        // de la tache, seule l'assignation change (statut et temps passe absents = conserves).
        TacheRequest requete = new TacheRequest();
        requete.setTitre(tache.getTitre());
        requete.setDescription(tache.getDescription());
        requete.setPriorite(tache.getPriorite() != null ? tache.getPriorite() : Priorite.NORMALE);
        requete.setTempsEstime(tache.getTempsEstime());
        requete.setIdSprint(tache.getSprint() != null ? tache.getSprint().getId() : null);
        requete.setDateEcheance(tache.getDateFinPrevue());
        requete.setIdsUtilisateursAssignes(Collections.singletonList(idAssigne));
        TacheResponse reponse = tacheService.modifier(projet.getId(), idTache, requete);

        return new SolutionTraiteeResponse(solution.getId(), solution.getLibelle(),
                TypeActionRecommandation.REASSIGNER_TACHE.name(), reponse.getId(), reponse.getCode(), false, null);
    }

    private SolutionTraiteeResponse creerTache(IaRecommandation solution, Risque risque, JsonNode action,
                                               boolean suivi, String remarqueImposee) {
        Projet projet = risque.getProjet();
        Long idAssigne = identifiant(action.path("idAssigne"));
        boolean assigneEligible = idAssigne != null && estMembreDeLEquipe(projet, idAssigne);
        String remarque = remarqueImposee;
        if (idAssigne != null && !assigneEligible && remarque == null) {
            remarque = REMARQUE_MEMBRE_HORS_EQUIPE;
        }

        String titre = texte(action.path("titre"));
        String description = texte(action.path("description"));
        if (titre == null) {
            titre = solution.getLibelle();
        }
        if (description == null) {
            description = solution.getJustification();
        }

        TacheRequest requete = new TacheRequest();
        requete.setTitre(tronquer(titre, MAX_LONGUEUR_TITRE_TACHE));
        requete.setDescription(descriptionAvecOrigine(description, risque));
        requete.setPriorite(prioriteSelonLeNiveau(risque.getNiveau()));
        requete.setTempsEstime(action.path("tempsEstime").isNumber() ? action.path("tempsEstime").doubleValue() : null);
        requete.setIdsUtilisateursAssignes(assigneEligible ? Collections.singletonList(idAssigne) : Collections.<Long>emptyList());
        TacheResponse reponse = tacheService.creer(projet.getId(), requete);

        return new SolutionTraiteeResponse(solution.getId(), solution.getLibelle(),
                TypeActionRecommandation.CREER_TACHE.name(), reponse.getId(), reponse.getCode(), suivi, remarque);
    }

    /** Les taches sont tracables : la description dit qu'elles viennent d'une solution IA et pour quel risque. */
    private String descriptionAvecOrigine(String description, Risque risque) {
        String origine = "Tâche créée à partir d'une solution proposée par l'assistant IA pour le risque « " + risque.getLibelle() + " ».";
        if (description == null) {
            return tronquer(origine, MAX_LONGUEUR_DESCRIPTION_TACHE);
        }
        int placeRestante = MAX_LONGUEUR_DESCRIPTION_TACHE - origine.length() - 2;
        if (placeRestante <= 0) {
            return tronquer(origine, MAX_LONGUEUR_DESCRIPTION_TACHE);
        }
        return tronquer(description, placeRestante) + "\n\n" + origine;
    }

    private Priorite prioriteSelonLeNiveau(NiveauRisque niveau) {
        return niveau == NiveauRisque.CRITIQUE || niveau == NiveauRisque.ELEVE ? Priorite.HAUTE : Priorite.NORMALE;
    }

    /** Meme regle que le TacheService : seul un membre enregistre de l'equipe du projet peut etre assigne. */
    private boolean estMembreDeLEquipe(Projet projet, Long idUtilisateur) {
        return projet.getEquipe() != null
                && membreEquipeRepository.existsByEquipeIdAndUtilisateurId(projet.getEquipe().getId(), idUtilisateur);
    }

    private JsonNode decrireResultat(SolutionTraiteeResponse traitee) {
        ObjectNode resultat = objectMapper.createObjectNode();
        resultat.put("typeAction", traitee.getTypeAction());
        resultat.put("idTache", traitee.getIdTache());
        resultat.put("codeTache", traitee.getCodeTache());
        resultat.put("suivi", traitee.isSuivi());
        if (traitee.getRemarque() != null) {
            resultat.put("remarque", traitee.getRemarque());
        }
        return resultat;
    }

    private Long identifiant(JsonNode noeud) {
        return noeud.isIntegralNumber() ? noeud.longValue() : null;
    }

    private String texte(JsonNode noeud) {
        return noeud.isTextual() && !noeud.asText().trim().isEmpty() ? noeud.asText().trim() : null;
    }

    private String tronquer(String valeur, int longueurMax) {
        return valeur.length() <= longueurMax ? valeur : valeur.substring(0, longueurMax - 1) + "…";
    }
}
