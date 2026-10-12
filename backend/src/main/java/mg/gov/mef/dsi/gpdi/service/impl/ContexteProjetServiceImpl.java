package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.BurndownSnapshot;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.Livrable;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutJalon;
import mg.gov.mef.dsi.gpdi.entity.StatutLivrable;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.BurndownSnapshotRepository;
import mg.gov.mef.dsi.gpdi.repository.JalonRepository;
import mg.gov.mef.dsi.gpdi.repository.LivrableRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.service.ContexteProjetService;
import mg.gov.mef.dsi.gpdi.util.ProgressionProjet;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Le contexte doit rester court (le modele travaille avec une fenetre de 8 192 jetons) : les listes
 * sont plafonnees et ordonnees pour que ce qui est le plus inquietant soit toujours transmis, le
 * reste n'etant fourni que sous forme de compteurs. Toujours en lecture seule.
 */
@Service
@RequiredArgsConstructor
public class ContexteProjetServiceImpl implements ContexteProjetService {

    private static final Set<StatutTache> STATUTS_TACHE_TERMINES = EnumSet.of(StatutTache.TERMINEE, StatutTache.REPORTEE);

    private static final int MAX_TACHES_A_SURVEILLER = 15;
    private static final int MAX_SPRINTS = 4;
    private static final int MAX_POINTS_BURNDOWN = 7;
    private static final int MAX_JALONS = 10;
    private static final int MAX_RISQUES_CONNUS = 30;
    private static final int MAX_LONGUEUR_TITRE = 80;
    /** Une tache dont l'echeance tombe dans ce delai (en jours) est deja a surveiller. */
    private static final int HORIZON_ECHEANCE_JOURS = 7;

    private static final String FONCTION_RESPONSABLE = "RESPONSABLE_PROJET";
    private static final String FONCTION_CHEF_EQUIPE = "CHEF_EQUIPE";
    private static final String FONCTION_MEMBRE = "MEMBRE";

    private final ProjetRepository projetRepository;
    private final TacheRepository tacheRepository;
    private final AffectationTacheRepository affectationTacheRepository;
    private final SprintRepository sprintRepository;
    private final BurndownSnapshotRepository burndownSnapshotRepository;
    private final JalonRepository jalonRepository;
    private final LivrableRepository livrableRepository;
    private final MembreEquipeRepository membreEquipeRepository;
    private final RisqueRepository risqueRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public ContexteProjetIa construire(Long idProjet) {
        Projet projet = projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));
        LocalDate aujourdhui = LocalDate.now();

        List<Tache> taches = tacheRepository.findByProjetId(idProjet, Pageable.unpaged()).getContent();
        Map<Long, List<Long>> assignesParTache = assignesParTache(taches);
        Map<Long, MembreContexte> membres = membresDeLEquipe(projet);
        List<Risque> risques = risqueRepository.findByProjetIdOrderByIdAsc(idProjet);

        ObjectNode racine = objectMapper.createObjectNode();
        racine.put("dateDuJour", aujourdhui.toString());
        racine.set("projet", decrireProjet(projet, taches, aujourdhui));
        racine.set("equipe", decrireEquipe(membres, taches, assignesParTache));
        racine.set("taches", decrireTaches(taches, assignesParTache, aujourdhui));
        racine.set("sprints", decrireSprints(idProjet, taches));
        racine.set("jalons", decrireJalons(idProjet, aujourdhui));
        racine.set("risquesConnus", decrireRisques(risques));

        Set<Long> idsRisquesReevaluables = risques.stream()
                .filter(risque -> risque.getStatut() == StatutRisque.SOUS_CONTROLE)
                .map(Risque::getId)
                .collect(Collectors.toSet());
        List<String> libellesRisquesConnus = risques.stream()
                .map(Risque::getLibelle)
                .collect(Collectors.toList());

        return new ContexteProjetIa(racine, new HashSet<>(membres.keySet()), idsRisquesReevaluables, libellesRisquesConnus);
    }

    private ObjectNode decrireProjet(Projet projet, List<Tache> taches, LocalDate aujourdhui) {
        ObjectNode noeud = objectMapper.createObjectNode();
        noeud.put("id", projet.getId());
        noeud.put("titre", projet.getTitre());
        noeud.put("statut", String.valueOf(projet.getStatut()));
        if (projet.getPriorite() != null) {
            noeud.put("priorite", projet.getPriorite().name());
        }
        if (projet.getDateDebut() != null) {
            noeud.put("dateDebut", projet.getDateDebut().toString());
        }
        if (projet.getDateFin() != null) {
            noeud.put("dateFin", projet.getDateFin().toString());
            noeud.put("joursAvantEcheance", ChronoUnit.DAYS.between(aujourdhui, projet.getDateFin()));
        }
        // Meme regle que l'API projets (ProgressionProjet) : la colonne projets.progression n'est pas tenue a jour.
        long terminees = taches.stream().filter(tache -> tache.getStatut() == StatutTache.TERMINEE).count();
        noeud.put("progressionPourcent", ProgressionProjet.calculer(terminees, taches.size()));
        return noeud;
    }

    private ArrayNode decrireEquipe(Map<Long, MembreContexte> membres, List<Tache> taches,
                                    Map<Long, List<Long>> assignesParTache) {
        Map<Long, Integer> tachesOuvertes = new HashMap<>();
        Map<Long, Double> heuresRestantes = new HashMap<>();
        for (Tache tache : taches) {
            if (!estOuverte(tache)) {
                continue;
            }
            for (Long idAssigne : assignesParTache.getOrDefault(tache.getId(), Collections.<Long>emptyList())) {
                tachesOuvertes.merge(idAssigne, 1, Integer::sum);
                heuresRestantes.merge(idAssigne, heuresRestantes(tache), Double::sum);
            }
        }

        ArrayNode equipe = objectMapper.createArrayNode();
        for (Map.Entry<Long, MembreContexte> entree : membres.entrySet()) {
            Utilisateur utilisateur = entree.getValue().utilisateur;
            ObjectNode noeud = objectMapper.createObjectNode();
            noeud.put("id", entree.getKey());
            noeud.put("nom", nomComplet(utilisateur));
            if (utilisateur.getRole() != null && utilisateur.getRole().getNom() != null) {
                noeud.put("role", utilisateur.getRole().getNom());
            }
            noeud.put("fonction", entree.getValue().fonction);
            noeud.put("tachesOuvertes", tachesOuvertes.getOrDefault(entree.getKey(), 0));
            noeud.put("heuresRestantes", arrondi(heuresRestantes.getOrDefault(entree.getKey(), 0.0)));
            equipe.add(noeud);
        }
        return equipe;
    }

    private ObjectNode decrireTaches(List<Tache> taches, Map<Long, List<Long>> assignesParTache, LocalDate aujourdhui) {
        Map<StatutTache, Integer> parStatut = new EnumMap<>(StatutTache.class);
        int enRetard = 0;
        int sansAssigne = 0;
        int sansEstimation = 0;
        int dansLeBacklog = 0;
        for (Tache tache : taches) {
            parStatut.merge(tache.getStatut(), 1, Integer::sum);
            if (!estOuverte(tache)) {
                continue;
            }
            if (estEnRetard(tache, aujourdhui)) {
                enRetard++;
            }
            if (assignesParTache.getOrDefault(tache.getId(), Collections.<Long>emptyList()).isEmpty()) {
                sansAssigne++;
            }
            if (tache.getTempsEstime() == null || tache.getTempsEstime() <= 0) {
                sansEstimation++;
            }
            if (tache.getSprint() == null) {
                dansLeBacklog++;
            }
        }

        ObjectNode statuts = objectMapper.createObjectNode();
        for (Map.Entry<StatutTache, Integer> entree : parStatut.entrySet()) {
            statuts.put(String.valueOf(entree.getKey()), entree.getValue());
        }

        List<Tache> aSurveiller = taches.stream()
                .filter(tache -> aSurveiller(tache, aujourdhui))
                .sorted(Comparator.comparingInt((Tache tache) -> rangDeSurveillance(tache, aujourdhui))
                        .thenComparing(Tache::getDateFinPrevue, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder())))
                .limit(MAX_TACHES_A_SURVEILLER)
                .collect(Collectors.toList());

        ArrayNode liste = objectMapper.createArrayNode();
        for (Tache tache : aSurveiller) {
            liste.add(decrireTache(tache, assignesParTache, aujourdhui));
        }

        ObjectNode noeud = objectMapper.createObjectNode();
        noeud.put("total", taches.size());
        noeud.set("parStatut", statuts);
        noeud.put("enRetard", enRetard);
        noeud.put("sansAssigne", sansAssigne);
        noeud.put("sansEstimation", sansEstimation);
        noeud.put("dansLeBacklog", dansLeBacklog);
        noeud.set("aSurveiller", liste);
        return noeud;
    }

    private ObjectNode decrireTache(Tache tache, Map<Long, List<Long>> assignesParTache, LocalDate aujourdhui) {
        ObjectNode noeud = objectMapper.createObjectNode();
        noeud.put("id", tache.getId());
        noeud.put("code", tache.getCode());
        noeud.put("titre", tronquer(tache.getTitre(), MAX_LONGUEUR_TITRE));
        noeud.put("statut", String.valueOf(tache.getStatut()));
        if (tache.getPriorite() != null) {
            noeud.put("priorite", tache.getPriorite().name());
        }
        if (tache.getDateFinPrevue() != null) {
            noeud.put("echeance", tache.getDateFinPrevue().toString());
            if (estOuverte(tache) && tache.getDateFinPrevue().isBefore(aujourdhui)) {
                noeud.put("joursDeRetard", ChronoUnit.DAYS.between(tache.getDateFinPrevue(), aujourdhui));
            }
        }
        if (tache.getSprint() != null) {
            noeud.put("idSprint", tache.getSprint().getId());
        }
        ArrayNode assignes = objectMapper.createArrayNode();
        for (Long idAssigne : assignesParTache.getOrDefault(tache.getId(), Collections.<Long>emptyList())) {
            assignes.add(idAssigne);
        }
        noeud.set("assignes", assignes);
        if (tache.getTempsEstime() != null) {
            noeud.put("heuresEstimees", arrondi(tache.getTempsEstime()));
        }
        return noeud;
    }

    private ArrayNode decrireSprints(Long idProjet, List<Tache> taches) {
        List<Sprint> sprints = sprintRepository.findByProjetId(idProjet);
        List<Sprint> nonClotures = sprints.stream()
                .filter(sprint -> sprint.getStatut() != StatutSprint.CLOTURER)
                .sorted(Comparator.comparing(Sprint::getDateDebut, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder())))
                .collect(Collectors.toList());
        List<Sprint> clotures = sprints.stream()
                .filter(sprint -> sprint.getStatut() == StatutSprint.CLOTURER)
                .sorted(Comparator.comparing(Sprint::getDateFin, Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())))
                .collect(Collectors.toList());
        List<Sprint> retenus = new ArrayList<>(nonClotures);
        retenus.addAll(clotures);

        ArrayNode liste = objectMapper.createArrayNode();
        for (Sprint sprint : retenus.stream().limit(MAX_SPRINTS).collect(Collectors.toList())) {
            double restant = taches.stream()
                    .filter(tache -> tache.getSprint() != null && sprint.getId().equals(tache.getSprint().getId()))
                    .filter(this::estOuverte)
                    .mapToDouble(this::heuresRestantes)
                    .sum();

            ObjectNode noeud = objectMapper.createObjectNode();
            noeud.put("id", sprint.getId());
            noeud.put("nom", sprint.getNom());
            noeud.put("statut", String.valueOf(sprint.getStatut()));
            if (sprint.getDateDebut() != null) {
                noeud.put("dateDebut", sprint.getDateDebut().toString());
            }
            if (sprint.getDateFin() != null) {
                noeud.put("dateFin", sprint.getDateFin().toString());
            }
            noeud.put("capaciteHeures", sprint.getCapaciteHeures() == null ? 0 : sprint.getCapaciteHeures());
            noeud.put("heuresRestantes", arrondi(restant));
            if (sprint.getStatut() == StatutSprint.EN_COURS) {
                noeud.set("burndown", decrireBurndown(sprint.getId()));
            }
            liste.add(noeud);
        }
        return liste;
    }

    private ArrayNode decrireBurndown(Long idSprint) {
        List<BurndownSnapshot> instantanes = burndownSnapshotRepository.findBySprintIdOrderByDateAsc(idSprint);
        int debut = Math.max(0, instantanes.size() - MAX_POINTS_BURNDOWN);

        ArrayNode points = objectMapper.createArrayNode();
        for (BurndownSnapshot instantane : instantanes.subList(debut, instantanes.size())) {
            ObjectNode point = objectMapper.createObjectNode();
            point.put("date", instantane.getDate().toString());
            point.put("heuresRestantes", arrondi(instantane.getHeuresRestantes() == null ? 0.0 : instantane.getHeuresRestantes()));
            points.add(point);
        }
        return points;
    }

    private ArrayNode decrireJalons(Long idProjet, LocalDate aujourdhui) {
        List<Jalon> jalons = jalonRepository.findByProjetId(idProjet, Pageable.unpaged()).getContent().stream()
                .sorted(Comparator.comparingInt((Jalon jalon) -> estJalonOuvert(jalon) ? 0 : 1)
                        .thenComparing(Jalon::getDateEcheance, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder())))
                .limit(MAX_JALONS)
                .collect(Collectors.toList());

        ArrayNode liste = objectMapper.createArrayNode();
        for (Jalon jalon : jalons) {
            ObjectNode noeud = objectMapper.createObjectNode();
            noeud.put("id", jalon.getId());
            noeud.put("titre", tronquer(jalon.getTitre(), MAX_LONGUEUR_TITRE));
            noeud.put("statut", String.valueOf(jalon.getStatut()));
            if (jalon.getDateEcheance() != null) {
                noeud.put("echeance", jalon.getDateEcheance().toString());
                noeud.put("joursAvantEcheance", ChronoUnit.DAYS.between(aujourdhui, jalon.getDateEcheance()));
            }
            noeud.set("livrables", compterLivrables(jalon.getId()));
            liste.add(noeud);
        }
        return liste;
    }

    private ObjectNode compterLivrables(Long idJalon) {
        Map<StatutLivrable, Integer> parStatut = new EnumMap<>(StatutLivrable.class);
        for (Livrable livrable : livrableRepository.findByJalonId(idJalon, Pageable.unpaged()).getContent()) {
            parStatut.merge(livrable.getStatut(), 1, Integer::sum);
        }
        ObjectNode noeud = objectMapper.createObjectNode();
        for (Map.Entry<StatutLivrable, Integer> entree : parStatut.entrySet()) {
            noeud.put(String.valueOf(entree.getKey()), entree.getValue());
        }
        return noeud;
    }

    /**
     * Tous les risques SOUS_CONTROLE sont transmis (l'IA doit pouvoir les reevaluer), completes par les
     * plus recents des autres jusqu'au plafond. Les doublons sont de toute facon filtres sur la liste
     * complete des libelles, pas seulement sur cet extrait.
     */
    private ArrayNode decrireRisques(List<Risque> risques) {
        List<Risque> retenus = new ArrayList<>();
        for (Risque risque : risques) {
            if (risque.getStatut() == StatutRisque.SOUS_CONTROLE) {
                retenus.add(risque);
            }
        }
        for (int i = risques.size() - 1; i >= 0 && retenus.size() < MAX_RISQUES_CONNUS; i--) {
            Risque risque = risques.get(i);
            if (risque.getStatut() != StatutRisque.SOUS_CONTROLE) {
                retenus.add(risque);
            }
        }
        retenus.sort(Comparator.comparing(Risque::getId));

        ArrayNode liste = objectMapper.createArrayNode();
        for (Risque risque : retenus) {
            ObjectNode noeud = objectMapper.createObjectNode();
            noeud.put("id", risque.getId());
            noeud.put("libelle", risque.getLibelle());
            noeud.put("statut", String.valueOf(risque.getStatut()));
            noeud.put("niveau", String.valueOf(risque.getNiveau()));
            noeud.put("probabilite", risque.getProbabilite());
            noeud.put("impact", risque.getImpact());
            liste.add(noeud);
        }
        return liste;
    }

    private Map<Long, List<Long>> assignesParTache(List<Tache> taches) {
        Map<Long, List<Long>> assignes = new HashMap<>();
        if (taches.isEmpty()) {
            return assignes;
        }
        List<Long> idsTaches = taches.stream().map(Tache::getId).collect(Collectors.toList());
        for (AffectationTache affectation : affectationTacheRepository.findByTacheIdIn(idsTaches)) {
            assignes.computeIfAbsent(affectation.getTache().getId(), cle -> new ArrayList<>())
                    .add(affectation.getUtilisateur().getId());
        }
        return assignes;
    }

    /** Responsable, chef d'equipe puis membres actifs ; un utilisateur cumulant plusieurs fonctions garde la premiere. */
    private Map<Long, MembreContexte> membresDeLEquipe(Projet projet) {
        Map<Long, MembreContexte> membres = new LinkedHashMap<>();
        ajouterMembre(membres, projet.getResponsable(), FONCTION_RESPONSABLE);
        if (projet.getEquipe() != null) {
            ajouterMembre(membres, projet.getEquipe().getChefEquipe(), FONCTION_CHEF_EQUIPE);
            for (MembreEquipe membre : membreEquipeRepository.findByEquipeId(projet.getEquipe().getId())) {
                if (Boolean.TRUE.equals(membre.getActif())) {
                    ajouterMembre(membres, membre.getUtilisateur(), FONCTION_MEMBRE);
                }
            }
        }
        return membres;
    }

    private void ajouterMembre(Map<Long, MembreContexte> membres, Utilisateur utilisateur, String fonction) {
        if (utilisateur != null && !membres.containsKey(utilisateur.getId())) {
            membres.put(utilisateur.getId(), new MembreContexte(utilisateur, fonction));
        }
    }

    private boolean estOuverte(Tache tache) {
        return !STATUTS_TACHE_TERMINES.contains(tache.getStatut());
    }

    private boolean estEnRetard(Tache tache, LocalDate aujourdhui) {
        return estOuverte(tache) && tache.getDateFinPrevue() != null && tache.getDateFinPrevue().isBefore(aujourdhui);
    }

    private boolean aSurveiller(Tache tache, LocalDate aujourdhui) {
        if (tache.getStatut() == StatutTache.BLOQUEE) {
            return true;
        }
        return estOuverte(tache) && tache.getDateFinPrevue() != null
                && !tache.getDateFinPrevue().isAfter(aujourdhui.plusDays(HORIZON_ECHEANCE_JOURS));
    }

    /** Bloquees d'abord, puis en retard, puis a echeance proche. */
    private int rangDeSurveillance(Tache tache, LocalDate aujourdhui) {
        if (tache.getStatut() == StatutTache.BLOQUEE) {
            return 0;
        }
        return estEnRetard(tache, aujourdhui) ? 1 : 2;
    }

    private boolean estJalonOuvert(Jalon jalon) {
        return jalon.getStatut() == StatutJalon.A_VENIR || jalon.getStatut() == StatutJalon.EN_RETARD;
    }

    private double heuresRestantes(Tache tache) {
        double estime = tache.getTempsEstime() == null ? 0.0 : tache.getTempsEstime();
        double passe = tache.getTempsPasse() == null ? 0.0 : tache.getTempsPasse();
        return Math.max(0.0, estime - passe);
    }

    private double arrondi(double valeur) {
        return Math.round(valeur * 10.0) / 10.0;
    }

    private String nomComplet(Utilisateur utilisateur) {
        String prenom = utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom();
        String nom = utilisateur.getNom() == null ? "" : utilisateur.getNom();
        return (prenom + " " + nom).trim();
    }

    private String tronquer(String texte, int longueurMax) {
        if (texte == null || texte.length() <= longueurMax) {
            return texte;
        }
        return texte.substring(0, longueurMax - 1) + "…";
    }

    private static final class MembreContexte {
        private final Utilisateur utilisateur;
        private final String fonction;

        private MembreContexte(Utilisateur utilisateur, String fonction) {
            this.utilisateur = utilisateur;
            this.fonction = fonction;
        }
    }
}
