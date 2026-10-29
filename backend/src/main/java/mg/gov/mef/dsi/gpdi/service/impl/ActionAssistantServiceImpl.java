package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.JalonRequest;
import mg.gov.mef.dsi.gpdi.dto.request.ProjetRequest;
import mg.gov.mef.dsi.gpdi.dto.request.SprintRequest;
import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.ActionAssistantResponse;
import mg.gov.mef.dsi.gpdi.dto.response.ProjetResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TacheResponse;
import mg.gov.mef.dsi.gpdi.entity.ActionAssistant;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutActionAssistant;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TypeActionAssistant;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.ActionAssistantImpossibleException;
import mg.gov.mef.dsi.gpdi.exception.ActionAssistantIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.JalonIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.RoleActionAssistantManquantException;
import mg.gov.mef.dsi.gpdi.exception.SprintIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutActionAssistantInvalideException;
import mg.gov.mef.dsi.gpdi.exception.TacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.repository.ActionAssistantRepository;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.JalonRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.ActionAssistantService;
import mg.gov.mef.dsi.gpdi.service.JalonService;
import mg.gov.mef.dsi.gpdi.service.ProjetService;
import mg.gov.mef.dsi.gpdi.service.SprintService;
import mg.gov.mef.dsi.gpdi.service.TacheService;
import mg.gov.mef.dsi.gpdi.util.CorrespondanceTexte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deux temps, comme les recommandations IA : la preparation ne fait que lire et verifier (aucune ecriture metier),
 * l'execution n'a lieu qu'a la confirmation, via les services metier existants. Les controles d'acces de ces
 * services (appartenance au projet, matrice d'habilitations, regles de gestion) s'appliquent donc tels quels ;
 * le role Keycloak de la route HTTP equivalente est en plus verifie, l'assistant ne passant pas par elle.
 */
@Service
@RequiredArgsConstructor
public class ActionAssistantServiceImpl implements ActionAssistantService {

    private static final Logger log = LoggerFactory.getLogger(ActionAssistantServiceImpl.class);

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern CODE_TACHE = Pattern.compile("^(?:t\\s*-?\\s*)?(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final int CAPACITE_SPRINT_PAR_DEFAUT = 40;
    private static final int MAX_NOMS_LISTES = 6;
    private static final int MAX_LONGUEUR_LIBELLE = 300;

    private static final Map<TypeActionAssistant, String> VERBES = new EnumMap<>(TypeActionAssistant.class);
    private static final Map<StatutTache, String> LIBELLES_STATUT = new EnumMap<>(StatutTache.class);
    private static final Map<Priorite, String> LIBELLES_PRIORITE = new EnumMap<>(Priorite.class);

    static {
        VERBES.put(TypeActionAssistant.DEMARRER_PROJET, "Démarrer le projet");
        VERBES.put(TypeActionAssistant.SUSPENDRE_PROJET, "Suspendre le projet");
        VERBES.put(TypeActionAssistant.REPRENDRE_PROJET, "Reprendre le projet");
        VERBES.put(TypeActionAssistant.CLOTURER_PROJET, "Clôturer le projet");
        VERBES.put(TypeActionAssistant.ARCHIVER_PROJET, "Archiver le projet");
        VERBES.put(TypeActionAssistant.DEMARRER_SPRINT, "Démarrer le sprint");
        VERBES.put(TypeActionAssistant.CLOTURER_SPRINT, "Clôturer le sprint");
        VERBES.put(TypeActionAssistant.SUPPRIMER_SPRINT, "Supprimer définitivement le sprint");
        VERBES.put(TypeActionAssistant.ATTEINDRE_JALON, "Marquer comme atteint le jalon");
        VERBES.put(TypeActionAssistant.ANNULER_JALON, "Annuler le jalon");
        VERBES.put(TypeActionAssistant.SUPPRIMER_JALON, "Supprimer définitivement le jalon");
        VERBES.put(TypeActionAssistant.ARCHIVER_TACHE, "Archiver la tâche");
        VERBES.put(TypeActionAssistant.RESTAURER_TACHE, "Restaurer la tâche");
        VERBES.put(TypeActionAssistant.SUPPRIMER_TACHE, "Supprimer définitivement la tâche");

        LIBELLES_STATUT.put(StatutTache.A_FAIRE, "À faire");
        LIBELLES_STATUT.put(StatutTache.EN_COURS, "En cours");
        LIBELLES_STATUT.put(StatutTache.EN_TEST, "En test");
        LIBELLES_STATUT.put(StatutTache.BLOQUEE, "Bloquée");
        LIBELLES_STATUT.put(StatutTache.TERMINEE, "Terminée");
        LIBELLES_STATUT.put(StatutTache.REPORTEE, "Reportée");

        LIBELLES_PRIORITE.put(Priorite.CRITIQUE, "Critique");
        LIBELLES_PRIORITE.put(Priorite.HAUTE, "Haute");
        LIBELLES_PRIORITE.put(Priorite.NORMALE, "Normale");
        LIBELLES_PRIORITE.put(Priorite.BASSE, "Basse");
    }

    private final ProjetRepository projetRepository;
    private final TacheRepository tacheRepository;
    private final SprintRepository sprintRepository;
    private final JalonRepository jalonRepository;
    private final EquipeRepository equipeRepository;
    private final MembreEquipeRepository membreEquipeRepository;
    private final AffectationTacheRepository affectationTacheRepository;
    private final ActionAssistantRepository actionAssistantRepository;
    private final ProjetService projetService;
    private final SprintService sprintService;
    private final JalonService jalonService;
    private final TacheService tacheService;
    private final UtilisateurCourantService utilisateurCourantService;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------ Preparation

    @Override
    @Transactional(readOnly = true)
    public ActionAssistant preparer(JsonNode actionModele, Utilisateur demandeur) {
        String typeTexte = texte(actionModele, "type");
        if (actionModele == null || !actionModele.isObject() || typeTexte == null) {
            return null;
        }
        TypeActionAssistant type = enumeration(TypeActionAssistant.class, typeTexte);
        if (type == null) {
            throw new ActionAssistantImpossibleException("Je ne sais pas encore effectuer cette action (« " + typeTexte + " »).");
        }

        Preparation p = new Preparation(actionModele, projetRepository.findByResponsableIdOrderByIdAsc(demandeur.getId()));
        switch (type) {
            case CREER_PROJET:
                preparerCreationProjet(p);
                break;
            case MODIFIER_PROJET:
                preparerModificationProjet(p);
                break;
            case DEMARRER_PROJET:
            case SUSPENDRE_PROJET:
            case REPRENDRE_PROJET:
            case CLOTURER_PROJET:
            case ARCHIVER_PROJET:
                p.projet(true);
                p.libelle = VERBES.get(type) + " « " + p.projet.getTitre() + " »";
                break;
            case CREER_SPRINT:
                preparerCreationSprint(p);
                break;
            case MODIFIER_SPRINT:
                preparerModificationSprint(p);
                break;
            case DEMARRER_SPRINT:
            case CLOTURER_SPRINT:
            case SUPPRIMER_SPRINT:
                p.sprint();
                p.libelle = VERBES.get(type) + " « " + p.sprint.getNom() + " »";
                p.detail("Projet : " + p.projet.getTitre());
                break;
            case CREER_JALON:
                preparerCreationJalon(p);
                break;
            case MODIFIER_JALON:
                preparerModificationJalon(p);
                break;
            case ATTEINDRE_JALON:
            case ANNULER_JALON:
            case SUPPRIMER_JALON:
                p.jalon();
                p.libelle = VERBES.get(type) + " « " + p.jalon.getTitre() + " »";
                p.detail("Projet : " + p.projet.getTitre());
                break;
            case CREER_TACHE:
                preparerCreationTache(p);
                break;
            case MODIFIER_TACHE:
                preparerModificationTache(p);
                break;
            case AFFECTER_TACHE:
            case DESAFFECTER_TACHE:
                preparerAffectation(p, type == TypeActionAssistant.AFFECTER_TACHE);
                break;
            case CHANGER_STATUT_TACHE:
                preparerChangementStatut(p);
                break;
            case ARCHIVER_TACHE:
            case RESTAURER_TACHE:
            case SUPPRIMER_TACHE:
                p.tache();
                p.libelle = VERBES.get(type) + " " + p.tache.getCode() + " — " + p.tache.getTitre();
                p.detail("Projet : " + p.projet.getTitre());
                break;
            default:
                throw new ActionAssistantImpossibleException("Je ne sais pas encore effectuer cette action.");
        }
        if (type.isIrreversible()) {
            p.detail("Action définitive : elle ne pourra pas être annulée.");
        }

        ActionAssistant action = new ActionAssistant();
        action.setDemandeur(demandeur);
        action.setType(type);
        action.setLibelle(tronquer(p.libelle, MAX_LONGUEUR_LIBELLE));
        action.setParametres(p.parametres());
        return action;
    }

    private void preparerCreationProjet(Preparation p) {
        String titre = p.requis("titre", "le titre du projet");
        LocalDate debut = p.dateRequise("dateDebut", "la date de début du projet");
        LocalDate fin = p.dateRequise("dateFin", "la date de fin du projet");
        Priorite priorite = p.priorite();
        p.valeurs.put("titre", titre);
        p.valeurs.put("dateDebut", debut.toString());
        p.valeurs.put("dateFin", fin.toString());
        p.valeurs.put("priorite", (priorite == null ? Priorite.NORMALE : priorite).name());
        p.texteOptionnel("description");
        p.equipeOptionnelle();
        p.libelle = "Créer le projet « " + titre + " »";
        p.detail("Période : du " + FORMAT_DATE.format(debut) + " au " + FORMAT_DATE.format(fin));
        p.detail("Priorité : " + LIBELLES_PRIORITE.get(priorite == null ? Priorite.NORMALE : priorite));
        p.detail("Responsable : vous");
    }

    private void preparerModificationProjet(Preparation p) {
        p.projet(true);
        p.libelle = "Modifier le projet « " + p.projet.getTitre() + " »";
        boolean modifie = p.texteOptionnel("titre") | p.texteOptionnel("description") | p.dateOptionnelle("dateDebut", "Début")
                | p.dateOptionnelle("dateFin", "Fin") | p.prioriteOptionnelle() | p.equipeOptionnelle();
        p.exigerModification(modifie, "du projet (titre, description, dates, priorité ou équipe)");
    }

    private void preparerCreationSprint(Preparation p) {
        p.projet(true);
        String nom = p.texte("nom");
        if (nom == null) {
            nom = "Sprint " + (sprintRepository.findByProjetId(p.projet.getId()).size() + 1);
        }
        LocalDate debut = p.dateRequise("dateDebut", "la date de début du sprint");
        LocalDate fin = p.dateRequise("dateFin", "la date de fin du sprint");
        Integer capacite = p.entier("capaciteHeures");
        p.valeurs.put("nom", nom);
        p.valeurs.put("dateDebut", debut.toString());
        p.valeurs.put("dateFin", fin.toString());
        p.valeurs.put("capaciteHeures", capacite == null ? CAPACITE_SPRINT_PAR_DEFAUT : capacite);
        p.texteOptionnel("objectif");
        p.libelle = "Créer le sprint « " + nom + " »";
        p.detail("Projet : " + p.projet.getTitre());
        p.detail("Période : du " + FORMAT_DATE.format(debut) + " au " + FORMAT_DATE.format(fin));
        p.detail("Capacité : " + (capacite == null ? CAPACITE_SPRINT_PAR_DEFAUT : capacite) + " h");
    }

    private void preparerModificationSprint(Preparation p) {
        p.sprint();
        p.libelle = "Modifier le sprint « " + p.sprint.getNom() + " »";
        p.detail("Projet : " + p.projet.getTitre());
        boolean modifie = p.texteOptionnel("nom") | p.dateOptionnelle("dateDebut", "Début") | p.dateOptionnelle("dateFin", "Fin")
                | p.entierOptionnel("capaciteHeures", "Capacité", " h") | p.texteOptionnel("objectif");
        p.exigerModification(modifie, "du sprint (nom, dates, capacité ou objectif)");
    }

    private void preparerCreationJalon(Preparation p) {
        p.projet(true);
        String titre = p.requis("titre", "le titre du jalon");
        LocalDate echeance = p.dateRequise("dateEcheance", "la date d'échéance du jalon");
        p.valeurs.put("titre", titre);
        p.valeurs.put("dateEcheance", echeance.toString());
        p.texteOptionnel("description");
        p.libelle = "Créer le jalon « " + titre + " »";
        p.detail("Projet : " + p.projet.getTitre());
        p.detail("Échéance : " + FORMAT_DATE.format(echeance));
    }

    private void preparerModificationJalon(Preparation p) {
        p.jalon();
        p.libelle = "Modifier le jalon « " + p.jalon.getTitre() + " »";
        p.detail("Projet : " + p.projet.getTitre());
        boolean modifie = p.texteOptionnel("titre") | p.texteOptionnel("description") | p.dateOptionnelle("dateEcheance", "Échéance");
        p.exigerModification(modifie, "du jalon (titre, description ou échéance)");
    }

    private void preparerCreationTache(Preparation p) {
        p.projet(true);
        String titre = p.requis("titre", "le titre de la tâche");
        Priorite priorite = p.priorite();
        p.valeurs.put("titre", titre);
        p.valeurs.put("priorite", (priorite == null ? Priorite.NORMALE : priorite).name());
        p.libelle = "Créer la tâche « " + titre + " »";
        p.detail("Projet : " + p.projet.getTitre());
        p.detail("Priorité : " + LIBELLES_PRIORITE.get(priorite == null ? Priorite.NORMALE : priorite));
        p.texteOptionnel("description");
        p.nombreOptionnel("heuresEstimees", "Estimation", " h");
        p.dateOptionnelle("dateEcheance", "Échéance");
        p.sprintOptionnel();
        List<Utilisateur> personnes = p.personnes(false);
        if (!personnes.isEmpty()) {
            p.valeurs.set("idsPersonnes", ids(personnes));
            p.detail("Assignés : " + noms(personnes));
        }
    }

    private void preparerModificationTache(Preparation p) {
        p.tache();
        p.libelle = "Modifier la tâche " + p.tache.getCode() + " — " + p.tache.getTitre();
        p.detail("Projet : " + p.projet.getTitre());
        boolean modifie = p.texteOptionnel("titre") | p.texteOptionnel("description") | p.prioriteOptionnelle()
                | p.nombreOptionnel("heuresEstimees", "Estimation", " h") | p.dateOptionnelle("dateEcheance", "Échéance")
                | p.sprintOptionnel();
        p.exigerModification(modifie, "de la tâche (titre, description, priorité, estimation, échéance ou sprint)");
    }

    private void preparerAffectation(Preparation p, boolean ajout) {
        p.tache();
        List<Utilisateur> personnes = p.personnes(true);
        p.valeurs.set("idsPersonnes", ids(personnes));
        p.libelle = (ajout ? "Affecter " + noms(personnes) + " à " : "Retirer " + noms(personnes) + " de ")
                + p.tache.getCode() + " — " + p.tache.getTitre();
        p.detail("Projet : " + p.projet.getTitre());
    }

    private void preparerChangementStatut(Preparation p) {
        p.tache();
        String statutTexte = p.requis("statut", "le nouveau statut de la tâche");
        StatutTache statut = enumeration(StatutTache.class, statutTexte);
        if (statut == null) {
            throw new ActionAssistantImpossibleException("Statut de tâche inconnu : « " + statutTexte
                    + " ». Statuts possibles : À faire, En cours, En test, Bloquée, Terminée, Reportée.");
        }
        p.valeurs.put("statut", statut.name());
        p.libelle = "Passer " + p.tache.getCode() + " — " + p.tache.getTitre() + " à « " + LIBELLES_STATUT.get(statut) + " »";
        p.detail("Projet : " + p.projet.getTitre());
        p.detail("Statut actuel : " + LIBELLES_STATUT.get(p.tache.getStatut()));
    }

    // ------------------------------------------------------------------ Confirmation / annulation

    @Override
    @Transactional
    public ActionAssistantResponse confirmer(Long idAction) {
        ActionAssistant action = obtenirActionProposee(idAction);
        verifierRoleKeycloak(action.getType().getRoleKeycloak());
        Resultat resultat = executer(action);

        action.setStatut(StatutActionAssistant.APPLIQUEE);
        action.setResultat(tronquer(resultat.message, 500));
        action.setDateTraitement(LocalDateTime.now());
        if (resultat.idProjet != null) {
            ((ObjectNode) action.getParametres().path("cible")).put("idProjet", resultat.idProjet);
        }
        ActionAssistant enregistree = actionAssistantRepository.save(action);
        log.info("Action {} ({}) de l'assistant IA confirmee", idAction, action.getType());
        return versResponse(enregistree);
    }

    @Override
    @Transactional
    public ActionAssistantResponse annuler(Long idAction) {
        ActionAssistant action = obtenirActionProposee(idAction);
        action.setStatut(StatutActionAssistant.ANNULEE);
        action.setResultat("Action annulée : rien n'a été modifié.");
        action.setDateTraitement(LocalDateTime.now());
        return versResponse(actionAssistantRepository.save(action));
    }

    private ActionAssistant obtenirActionProposee(Long idAction) {
        Utilisateur courant = utilisateurCourantService.obtenirUtilisateurCourant();
        ActionAssistant action = actionAssistantRepository.findById(idAction)
                .filter(a -> a.getDemandeur() != null && a.getDemandeur().getId().equals(courant.getId()))
                .orElseThrow(() -> new ActionAssistantIntrouvableException(idAction));
        if (action.getStatut() != StatutActionAssistant.PROPOSEE) {
            throw new StatutActionAssistantInvalideException(idAction, action.getStatut());
        }
        return action;
    }

    private void verifierRoleKeycloak(String role) {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        boolean present = authentification != null && authentification.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(autorite -> autorite.equals("ROLE_" + role));
        if (!present) {
            throw new RoleActionAssistantManquantException(role);
        }
    }

    // ------------------------------------------------------------------ Execution

    private Resultat executer(ActionAssistant action) {
        JsonNode cible = action.getParametres().path("cible");
        JsonNode valeurs = action.getParametres().path("valeurs");
        Long idProjet = idOuNull(cible, "idProjet");
        if (idProjet != null) {
            verifierProjetDuDemandeur(idProjet, action.getDemandeur());
        }

        switch (action.getType()) {
            case CREER_PROJET: {
                ProjetRequest request = new ProjetRequest();
                request.setTitre(valeurs.path("titre").asText());
                request.setDescription(texte(valeurs, "description"));
                request.setDateDebut(LocalDate.parse(valeurs.path("dateDebut").asText()));
                request.setDateFin(LocalDate.parse(valeurs.path("dateFin").asText()));
                request.setPriorite(Priorite.valueOf(valeurs.path("priorite").asText()));
                request.setIdResponsable(action.getDemandeur().getId());
                request.setIdEquipe(idOuNull(valeurs, "idEquipe"));
                ProjetResponse cree = projetService.creer(request);
                return new Resultat("Projet « " + cree.getTitre() + " » créé (" + cree.getCodeReference() + ").", cree.getId());
            }
            case MODIFIER_PROJET: {
                Projet projet = projetRepository.findById(idProjet).orElseThrow(() -> new ProjetIntrouvableException(idProjet));
                ProjetRequest request = new ProjetRequest();
                request.setTitre(texteOu(valeurs, "titre", projet.getTitre()));
                request.setDescription(texteOu(valeurs, "description", projet.getDescription()));
                request.setDateDebut(dateOu(valeurs, "dateDebut", projet.getDateDebut()));
                request.setDateFin(dateOu(valeurs, "dateFin", projet.getDateFin()));
                request.setPriorite(valeurs.hasNonNull("priorite") ? Priorite.valueOf(valeurs.path("priorite").asText()) : projet.getPriorite());
                request.setIdResponsable(projet.getResponsable().getId());
                request.setIdEquipe(valeurs.hasNonNull("idEquipe") ? valeurs.path("idEquipe").asLong()
                        : (projet.getEquipe() != null ? projet.getEquipe().getId() : null));
                projetService.modifier(idProjet, request);
                return new Resultat("Projet « " + request.getTitre() + " » modifié.", idProjet);
            }
            case DEMARRER_PROJET:
                return new Resultat("Projet « " + projetService.demarrer(idProjet).getTitre() + " » démarré.", idProjet);
            case SUSPENDRE_PROJET:
                return new Resultat("Projet « " + projetService.suspendre(idProjet).getTitre() + " » suspendu.", idProjet);
            case REPRENDRE_PROJET:
                return new Resultat("Projet « " + projetService.reprendre(idProjet).getTitre() + " » repris.", idProjet);
            case CLOTURER_PROJET:
                return new Resultat("Projet « " + projetService.cloturer(idProjet).getTitre() + " » clôturé.", idProjet);
            case ARCHIVER_PROJET:
                return new Resultat("Projet « " + projetService.archiver(idProjet).getTitre() + " » archivé.", idProjet);

            case CREER_SPRINT: {
                SprintRequest request = new SprintRequest();
                request.setNom(valeurs.path("nom").asText());
                request.setDateDebut(LocalDate.parse(valeurs.path("dateDebut").asText()));
                request.setDateFin(LocalDate.parse(valeurs.path("dateFin").asText()));
                request.setCapaciteHeures(valeurs.path("capaciteHeures").asInt());
                request.setObjectif(texte(valeurs, "objectif"));
                return new Resultat("Sprint « " + sprintService.creer(idProjet, request).getNom() + " » créé.", idProjet);
            }
            case MODIFIER_SPRINT: {
                Long idSprint = cible.path("idSprint").asLong();
                Sprint sprint = sprintRepository.findById(idSprint).orElseThrow(() -> new SprintIntrouvableException(idSprint));
                SprintRequest request = new SprintRequest();
                request.setNom(texteOu(valeurs, "nom", sprint.getNom()));
                request.setDateDebut(dateOu(valeurs, "dateDebut", sprint.getDateDebut()));
                request.setDateFin(dateOu(valeurs, "dateFin", sprint.getDateFin()));
                request.setCapaciteHeures(valeurs.hasNonNull("capaciteHeures") ? valeurs.path("capaciteHeures").asInt() : sprint.getCapaciteHeures());
                request.setObjectif(texteOu(valeurs, "objectif", sprint.getObjectif()));
                return new Resultat("Sprint « " + sprintService.modifier(idProjet, idSprint, request).getNom() + " » modifié.", idProjet);
            }
            case DEMARRER_SPRINT:
                return new Resultat("Sprint « " + sprintService.commencer(idProjet, cible.path("idSprint").asLong()).getNom() + " » démarré.", idProjet);
            case CLOTURER_SPRINT:
                return new Resultat("Sprint « " + sprintService.terminer(idProjet, cible.path("idSprint").asLong()).getNom() + " » clôturé.", idProjet);
            case SUPPRIMER_SPRINT:
                sprintService.supprimer(idProjet, cible.path("idSprint").asLong());
                return new Resultat("Sprint supprimé.", idProjet);

            case CREER_JALON: {
                JalonRequest request = new JalonRequest();
                request.setTitre(valeurs.path("titre").asText());
                request.setDescription(texte(valeurs, "description"));
                request.setDateEcheance(LocalDate.parse(valeurs.path("dateEcheance").asText()));
                return new Resultat("Jalon « " + jalonService.creer(idProjet, request).getTitre() + " » créé.", idProjet);
            }
            case MODIFIER_JALON: {
                Long idJalon = cible.path("idJalon").asLong();
                Jalon jalon = jalonRepository.findById(idJalon).orElseThrow(() -> new JalonIntrouvableException(idJalon));
                JalonRequest request = new JalonRequest();
                request.setTitre(texteOu(valeurs, "titre", jalon.getTitre()));
                request.setDescription(texteOu(valeurs, "description", jalon.getDescription()));
                request.setDateEcheance(dateOu(valeurs, "dateEcheance", jalon.getDateEcheance()));
                return new Resultat("Jalon « " + jalonService.modifier(idProjet, idJalon, request).getTitre() + " » modifié.", idProjet);
            }
            case ATTEINDRE_JALON:
                return new Resultat("Jalon « " + jalonService.terminer(idProjet, cible.path("idJalon").asLong()).getTitre() + " » atteint.", idProjet);
            case ANNULER_JALON:
                return new Resultat("Jalon « " + jalonService.annuler(idProjet, cible.path("idJalon").asLong()).getTitre() + " » annulé.", idProjet);
            case SUPPRIMER_JALON:
                jalonService.supprimer(idProjet, cible.path("idJalon").asLong());
                return new Resultat("Jalon supprimé.", idProjet);

            case CREER_TACHE: {
                TacheRequest request = new TacheRequest();
                request.setTitre(valeurs.path("titre").asText());
                request.setDescription(texte(valeurs, "description"));
                request.setPriorite(Priorite.valueOf(valeurs.path("priorite").asText()));
                request.setTempsEstime(valeurs.hasNonNull("heuresEstimees") ? valeurs.path("heuresEstimees").asDouble() : null);
                request.setDateEcheance(dateOu(valeurs, "dateEcheance", null));
                request.setIdSprint(idOuNull(valeurs, "idSprint"));
                request.setIdsUtilisateursAssignes(listeIds(valeurs.path("idsPersonnes")));
                TacheResponse creee = tacheService.creer(idProjet, request);
                return new Resultat("Tâche " + creee.getCode() + " « " + creee.getTitre() + " » créée.", idProjet);
            }
            case MODIFIER_TACHE:
            case AFFECTER_TACHE:
            case DESAFFECTER_TACHE:
            case CHANGER_STATUT_TACHE:
                return modifierTache(action.getType(), idProjet, cible.path("idTache").asLong(), valeurs);
            case ARCHIVER_TACHE:
                return new Resultat("Tâche " + tacheService.archiver(idProjet, cible.path("idTache").asLong()).getCode() + " archivée.", idProjet);
            case RESTAURER_TACHE:
                return new Resultat("Tâche " + tacheService.restaurer(idProjet, cible.path("idTache").asLong()).getCode() + " restaurée dans le Kanban.", idProjet);
            case SUPPRIMER_TACHE:
                tacheService.supprimer(idProjet, cible.path("idTache").asLong());
                return new Resultat("Tâche supprimée.", idProjet);
            default:
                throw new ActionAssistantImpossibleException("Action non prise en charge.");
        }
    }

    /**
     * Le PUT d'une tache remplace tous ses champs : la requete part de l'etat courant de la tache (relu a la
     * confirmation, pas a la proposition) et n'y change que ce que l'action demande. Le statut n'est envoye que
     * pour un changement de statut (absent = conserve, donc chronometre et dependances non touches).
     */
    private Resultat modifierTache(TypeActionAssistant type, Long idProjet, Long idTache, JsonNode valeurs) {
        Tache tache = tacheRepository.findById(idTache).orElseThrow(() -> new TacheIntrouvableException(idTache));
        Set<Long> assignes = affectationTacheRepository.findByTacheId(idTache).stream()
                .map(affectation -> affectation.getUtilisateur().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        TacheRequest request = new TacheRequest();
        request.setTitre(texteOu(valeurs, "titre", tache.getTitre()));
        request.setDescription(texteOu(valeurs, "description", tache.getDescription()));
        request.setPriorite(valeurs.hasNonNull("priorite") ? Priorite.valueOf(valeurs.path("priorite").asText()) : tache.getPriorite());
        request.setTempsEstime(valeurs.hasNonNull("heuresEstimees") ? Double.valueOf(valeurs.path("heuresEstimees").asDouble()) : tache.getTempsEstime());
        request.setDateEcheance(dateOu(valeurs, "dateEcheance", tache.getDateFinPrevue()));
        Long idSprintCourant = tache.getSprint() != null ? tache.getSprint().getId() : null;
        if (valeurs.path("retirerDuSprint").asBoolean(false)) {
            request.setIdSprint(null);
        } else {
            request.setIdSprint(valeurs.hasNonNull("idSprint") ? Long.valueOf(valeurs.path("idSprint").asLong()) : idSprintCourant);
        }
        if (type == TypeActionAssistant.AFFECTER_TACHE) {
            assignes.addAll(listeIds(valeurs.path("idsPersonnes")));
        } else if (type == TypeActionAssistant.DESAFFECTER_TACHE) {
            assignes.removeAll(listeIds(valeurs.path("idsPersonnes")));
        }
        request.setIdsUtilisateursAssignes(new ArrayList<>(assignes));
        if (type == TypeActionAssistant.CHANGER_STATUT_TACHE) {
            request.setStatut(StatutTache.valueOf(valeurs.path("statut").asText()));
        }

        TacheResponse modifiee = tacheService.modifier(idProjet, idTache, request);
        String suite;
        if (type == TypeActionAssistant.AFFECTER_TACHE) {
            suite = " : affectation mise à jour.";
        } else if (type == TypeActionAssistant.DESAFFECTER_TACHE) {
            suite = " : affectation retirée.";
        } else if (type == TypeActionAssistant.CHANGER_STATUT_TACHE) {
            suite = " passée à « " + LIBELLES_STATUT.get(modifiee.getStatut()) + " ».";
        } else {
            suite = " modifiée.";
        }
        return new Resultat("Tâche " + modifiee.getCode() + suite, idProjet);
    }

    private void verifierProjetDuDemandeur(Long idProjet, Utilisateur demandeur) {
        Projet projet = projetRepository.findById(idProjet).orElseThrow(() -> new ProjetIntrouvableException(idProjet));
        if (projet.getResponsable() == null || !projet.getResponsable().getId().equals(demandeur.getId())) {
            throw new ActionAssistantImpossibleException("Vous n'êtes plus responsable du projet « " + projet.getTitre() + " » : action impossible.");
        }
    }

    // ------------------------------------------------------------------ Reponses

    @Override
    public ActionAssistantResponse versResponse(ActionAssistant action) {
        ActionAssistantResponse reponse = new ActionAssistantResponse();
        reponse.setId(action.getId());
        reponse.setType(action.getType());
        reponse.setLibelle(action.getLibelle());
        List<String> details = new ArrayList<>();
        for (JsonNode ligne : action.getParametres().path("details")) {
            details.add(ligne.asText());
        }
        reponse.setDetails(details);
        reponse.setIrreversible(action.getType().isIrreversible());
        reponse.setStatut(action.getStatut());
        reponse.setResultat(action.getResultat());
        reponse.setIdProjet(idOuNull(action.getParametres().path("cible"), "idProjet"));
        reponse.setDateTraitement(action.getDateTraitement());
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ActionAssistantResponse> parAnalyse(List<Long> idsAnalyses) {
        Map<Long, ActionAssistantResponse> resultat = new HashMap<>();
        if (idsAnalyses.isEmpty()) {
            return resultat;
        }
        for (ActionAssistant action : actionAssistantRepository.findByAnalyseIdIn(idsAnalyses)) {
            resultat.put(action.getAnalyse().getId(), versResponse(action));
        }
        return resultat;
    }

    // ------------------------------------------------------------------ Outils

    /** Etat d'une preparation : designations resolues, valeurs a appliquer et lignes affichees a l'utilisateur. */
    private final class Preparation {
        private final JsonNode modele;
        private final List<Projet> projets;
        private final ObjectNode cible = objectMapper.createObjectNode();
        private final ObjectNode valeurs = objectMapper.createObjectNode();
        private final ArrayNode details = objectMapper.createArrayNode();
        private String libelle;
        private Projet projet;
        private Tache tache;
        private Sprint sprint;
        private Jalon jalon;

        private Preparation(JsonNode modele, List<Projet> projets) {
            this.modele = modele;
            this.projets = projets;
        }

        private ObjectNode parametres() {
            ObjectNode racine = objectMapper.createObjectNode();
            racine.set("cible", cible);
            racine.set("valeurs", valeurs);
            racine.set("details", details);
            return racine;
        }

        private void detail(String ligne) {
            details.add(ligne);
        }

        private String texte(String champ) {
            return ActionAssistantServiceImpl.texte(modele, champ);
        }

        private String requis(String champ, String designation) {
            String valeur = texte(champ);
            if (valeur == null) {
                throw new ActionAssistantImpossibleException("Il me manque " + designation + " pour préparer cette action.");
            }
            return valeur;
        }

        /** Projet designe, ou le seul projet de l'utilisateur ; sans designation, plusieurs projets = ambigu. */
        private void projet(boolean requis) {
            if (projet != null) {
                return;
            }
            if (projets.isEmpty()) {
                throw new ActionAssistantImpossibleException("Vous n'êtes responsable d'aucun projet.");
            }
            String nom = texte("projet");
            if (nom == null) {
                if (projets.size() == 1 || !requis) {
                    projet = projets.size() == 1 ? projets.get(0) : null;
                    if (projet != null) {
                        cible.put("idProjet", projet.getId());
                    }
                    return;
                }
                throw new ActionAssistantImpossibleException("Pour quel projet ? Vos projets : " + liste(projets, Projet::getTitre) + ".");
            }
            List<Projet> trouves = CorrespondanceTexte.meilleurs(nom, projets, p -> p.getTitre());
            if (trouves.isEmpty()) {
                trouves = projets.stream().filter(p -> nom.equalsIgnoreCase(p.getCodeReference())).collect(Collectors.toList());
            }
            projet = unique(trouves, "projet", nom, Projet::getTitre, projets);
            cible.put("idProjet", projet.getId());
        }

        private void tache() {
            String designation = requis("tache", "la tâche concernée (son code, par exemple T-12)");
            projet(false);
            List<Tache> candidates = new ArrayList<>();
            for (Projet p : projet != null ? Collections.singletonList(projet) : projets) {
                candidates.addAll(tacheRepository.findByProjetId(p.getId(), Pageable.unpaged()).getContent());
            }
            Matcher code = CODE_TACHE.matcher(designation.trim());
            List<Tache> trouvees = code.matches()
                    ? candidates.stream().filter(t -> ("T-" + code.group(1)).equalsIgnoreCase(t.getCode())).collect(Collectors.toList())
                    : CorrespondanceTexte.meilleurs(designation, candidates, Tache::getTitre);
            tache = unique(trouvees, "tâche", designation, t -> t.getCode() + " " + t.getTitre(), null);
            projet = tache.getProjet();
            cible.put("idProjet", projet.getId());
            cible.put("idTache", tache.getId());
        }

        private void sprint() {
            String designation = requis("sprint", "le nom du sprint concerné");
            sprint = sprintDesigne(designation);
            projet = sprint.getProjet();
            cible.put("idProjet", projet.getId());
            cible.put("idSprint", sprint.getId());
        }

        private Sprint sprintDesigne(String designation) {
            projet(false);
            List<Sprint> candidats = new ArrayList<>();
            for (Projet p : projet != null ? Collections.singletonList(projet) : projets) {
                candidats.addAll(sprintRepository.findByProjetId(p.getId()));
            }
            String normalise = CorrespondanceTexte.normaliser(designation);
            List<Sprint> trouves = normalise.matches("sprint (en cours|actuel|courant)|(en cours|actuel|courant)")
                    ? candidats.stream().filter(s -> s.getStatut() == StatutSprint.EN_COURS).collect(Collectors.toList())
                    : CorrespondanceTexte.meilleurs(designation, candidats, Sprint::getNom);
            return unique(trouves, "sprint", designation, s -> s.getNom() + " (" + s.getProjet().getTitre() + ")", candidats);
        }

        private boolean sprintOptionnel() {
            String designation = texte("sprint");
            if (designation == null) {
                return false;
            }
            if (CorrespondanceTexte.normaliser(designation).matches("backlog|aucun|aucun sprint")) {
                valeurs.put("retirerDuSprint", true);
                detail("Sprint : aucun (backlog)");
                return true;
            }
            Long idProjetAvant = projet != null ? projet.getId() : null;
            Sprint trouve = sprintDesigne(designation);
            if (idProjetAvant != null && !trouve.getProjet().getId().equals(idProjetAvant)) {
                throw new ActionAssistantImpossibleException("Le sprint « " + trouve.getNom() + " » n'appartient pas au projet « " + projet.getTitre() + " ».");
            }
            valeurs.put("idSprint", trouve.getId());
            detail("Sprint : " + trouve.getNom());
            return true;
        }

        private void jalon() {
            String designation = requis("jalon", "le titre du jalon concerné");
            projet(false);
            List<Jalon> candidats = new ArrayList<>();
            for (Projet p : projet != null ? Collections.singletonList(projet) : projets) {
                candidats.addAll(jalonRepository.findByProjetId(p.getId()));
            }
            jalon = unique(CorrespondanceTexte.meilleurs(designation, candidats, Jalon::getTitre), "jalon", designation,
                    j -> j.getTitre() + " (" + j.getProjet().getTitre() + ")", candidats);
            projet = jalon.getProjet();
            cible.put("idProjet", projet.getId());
            cible.put("idJalon", jalon.getId());
        }

        /** Personnes designees parmi le responsable, le chef d'equipe et les membres actifs de l'equipe du projet. */
        private List<Utilisateur> personnes(boolean requis) {
            List<String> noms = new ArrayList<>();
            JsonNode noeud = modele.path("personnes");
            if (noeud.isArray()) {
                noeud.forEach(n -> {
                    if (n.isTextual() && StringUtils.hasText(n.asText())) {
                        noms.add(n.asText().trim());
                    }
                });
            } else if (noeud.isTextual() && StringUtils.hasText(noeud.asText())) {
                noms.add(noeud.asText().trim());
            }
            if (noms.isEmpty()) {
                if (requis) {
                    throw new ActionAssistantImpossibleException("Quelle(s) personne(s) faut-il affecter ou retirer ?");
                }
                return Collections.emptyList();
            }
            Map<Long, Utilisateur> equipe = new LinkedHashMap<>();
            ajouter(equipe, projet.getResponsable());
            if (projet.getEquipe() != null) {
                ajouter(equipe, projet.getEquipe().getChefEquipe());
                for (MembreEquipe membre : membreEquipeRepository.findByEquipeId(projet.getEquipe().getId())) {
                    if (Boolean.TRUE.equals(membre.getActif())) {
                        ajouter(equipe, membre.getUtilisateur());
                    }
                }
            }
            List<Utilisateur> candidats = new ArrayList<>(equipe.values());
            List<Utilisateur> resolues = new ArrayList<>();
            for (String nom : noms) {
                Utilisateur personne = unique(CorrespondanceTexte.meilleurs(nom, candidats, ActionAssistantServiceImpl::nomComplet),
                        "membre de l'équipe du projet « " + projet.getTitre() + " »", nom, ActionAssistantServiceImpl::nomComplet, candidats);
                if (!resolues.contains(personne)) {
                    resolues.add(personne);
                }
            }
            return resolues;
        }

        private boolean equipeOptionnelle() {
            String nom = texte("equipe");
            if (nom == null) {
                return false;
            }
            List<Equipe> equipes = equipeRepository.findAll();
            Equipe equipe = unique(CorrespondanceTexte.meilleurs(nom, equipes, Equipe::getNom), "équipe", nom, Equipe::getNom, equipes);
            valeurs.put("idEquipe", equipe.getId());
            detail("Équipe : " + equipe.getNom());
            return true;
        }

        private boolean texteOptionnel(String champ) {
            String valeur = texte(champ);
            if (valeur == null) {
                return false;
            }
            valeurs.put(champ, valeur);
            if (!"description".equals(champ) && !"objectif".equals(champ)) {
                detail(Character.toUpperCase(champ.charAt(0)) + champ.substring(1) + " : " + valeur);
            } else {
                detail(("description".equals(champ) ? "Description" : "Objectif") + " : " + tronquer(valeur, 120));
            }
            return true;
        }

        private LocalDate dateRequise(String champ, String designation) {
            LocalDate date = date(champ);
            if (date == null) {
                throw new ActionAssistantImpossibleException("Il me manque " + designation + " (format AAAA-MM-JJ) pour préparer cette action.");
            }
            return date;
        }

        private boolean dateOptionnelle(String champ, String libelleChamp) {
            LocalDate date = date(champ);
            if (date == null) {
                return false;
            }
            valeurs.put(champ, date.toString());
            detail(libelleChamp + " : " + FORMAT_DATE.format(date));
            return true;
        }

        private LocalDate date(String champ) {
            String valeur = texte(champ);
            if (valeur == null) {
                return null;
            }
            try {
                return LocalDate.parse(valeur.length() > 10 ? valeur.substring(0, 10) : valeur);
            } catch (DateTimeParseException e) {
                throw new ActionAssistantImpossibleException("Date non reconnue : « " + valeur + " ». Indiquez-la au format AAAA-MM-JJ.");
            }
        }

        private Priorite priorite() {
            String valeur = texte("priorite");
            if (valeur == null) {
                return null;
            }
            Priorite priorite = enumeration(Priorite.class, valeur);
            if (priorite == null) {
                throw new ActionAssistantImpossibleException("Priorité inconnue : « " + valeur + " ». Priorités possibles : Critique, Haute, Normale, Basse.");
            }
            return priorite;
        }

        private boolean prioriteOptionnelle() {
            Priorite priorite = priorite();
            if (priorite == null) {
                return false;
            }
            valeurs.put("priorite", priorite.name());
            detail("Priorité : " + LIBELLES_PRIORITE.get(priorite));
            return true;
        }

        private Double nombre(String champ) {
            JsonNode noeud = modele.path(champ);
            if (noeud.isNumber()) {
                return noeud.asDouble() > 0 ? noeud.asDouble() : null;
            }
            String valeur = texte(champ);
            if (valeur == null) {
                return null;
            }
            try {
                double nombre = Double.parseDouble(valeur.replace(',', '.').replaceAll("[^0-9.]", ""));
                return nombre > 0 ? nombre : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private Integer entier(String champ) {
            Double valeur = nombre(champ);
            return valeur == null ? null : (int) Math.round(valeur);
        }

        private boolean nombreOptionnel(String champ, String libelleChamp, String unite) {
            Double valeur = nombre(champ);
            if (valeur == null) {
                return false;
            }
            valeurs.put(champ, valeur);
            detail(libelleChamp + " : " + (valeur % 1 == 0 ? String.valueOf(valeur.intValue()) : String.valueOf(valeur)) + unite);
            return true;
        }

        private boolean entierOptionnel(String champ, String libelleChamp, String unite) {
            Integer valeur = entier(champ);
            if (valeur == null) {
                return false;
            }
            valeurs.put(champ, valeur);
            detail(libelleChamp + " : " + valeur + unite);
            return true;
        }

        private void exigerModification(boolean modifie, String quoi) {
            if (!modifie) {
                throw new ActionAssistantImpossibleException("Que faut-il modifier ? Précisez la nouvelle valeur " + quoi + ".");
            }
        }
    }

    /** Un seul element attendu : aucun = introuvable, plusieurs = designation ambigue a preciser. */
    private static <T> T unique(List<T> trouves, String nature, String designation, Function<T, String> libelle, List<T> tous) {
        if (trouves.size() == 1) {
            return trouves.get(0);
        }
        if (trouves.isEmpty()) {
            String suggestion = tous == null || tous.isEmpty() ? "" : " Possibilités : " + liste(tous, libelle) + ".";
            throw new ActionAssistantImpossibleException("Je ne trouve aucun(e) " + nature + " correspondant à « " + designation + " »." + suggestion);
        }
        throw new ActionAssistantImpossibleException("Plusieurs éléments correspondent à « " + designation + " » : "
                + liste(trouves, libelle) + ". Lequel voulez-vous ?");
    }

    private static <T> String liste(List<T> elements, Function<T, String> libelle) {
        String texte = elements.stream().limit(MAX_NOMS_LISTES).map(libelle).collect(Collectors.joining(", "));
        return elements.size() > MAX_NOMS_LISTES ? texte + "…" : texte;
    }

    private static void ajouter(Map<Long, Utilisateur> equipe, Utilisateur utilisateur) {
        if (utilisateur != null) {
            equipe.putIfAbsent(utilisateur.getId(), utilisateur);
        }
    }

    private static String nomComplet(Utilisateur utilisateur) {
        String prenom = utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom();
        String nom = utilisateur.getNom() == null ? "" : utilisateur.getNom();
        return (prenom + " " + nom).trim();
    }

    private static String noms(List<Utilisateur> personnes) {
        return personnes.stream().map(ActionAssistantServiceImpl::nomComplet).collect(Collectors.joining(", "));
    }

    private ArrayNode ids(List<Utilisateur> personnes) {
        ArrayNode noeud = objectMapper.createArrayNode();
        personnes.forEach(p -> noeud.add(p.getId()));
        return noeud;
    }

    private static List<Long> listeIds(JsonNode noeud) {
        List<Long> ids = new ArrayList<>();
        noeud.forEach(n -> ids.add(n.asLong()));
        return ids;
    }

    /** Accepte « EN_COURS », « en cours », « En cours » : comparaison sans accents ni casse. */
    private static <E extends Enum<E>> E enumeration(Class<E> type, String valeur) {
        String normalise = CorrespondanceTexte.normaliser(valeur).replace(' ', '_').toUpperCase(Locale.ROOT);
        for (E constante : type.getEnumConstants()) {
            if (constante.name().equals(normalise)) {
                return constante;
            }
        }
        return null;
    }

    private static String texte(JsonNode noeud, String champ) {
        if (noeud == null) {
            return null;
        }
        JsonNode valeur = noeud.path(champ);
        return valeur.isValueNode() && StringUtils.hasText(valeur.asText()) && !"null".equals(valeur.asText())
                ? valeur.asText().trim() : null;
    }

    private static String texteOu(JsonNode valeurs, String champ, String defaut) {
        String valeur = texte(valeurs, champ);
        return valeur != null ? valeur : defaut;
    }

    private static LocalDate dateOu(JsonNode valeurs, String champ, LocalDate defaut) {
        String valeur = texte(valeurs, champ);
        return valeur != null ? LocalDate.parse(valeur) : defaut;
    }

    private static Long idOuNull(JsonNode noeud, String champ) {
        return noeud.hasNonNull(champ) ? Long.valueOf(noeud.path(champ).asLong()) : null;
    }

    private static String tronquer(String valeur, int longueurMax) {
        if (valeur == null || valeur.length() <= longueurMax) {
            return valeur;
        }
        return valeur.substring(0, longueurMax - 1) + "…";
    }

    private static final class Resultat {
        private final String message;
        private final Long idProjet;

        private Resultat(String message, Long idProjet) {
            this.message = message;
            this.idProjet = idProjet;
        }
    }
}
