package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseRecommandationProjet;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseRecommandationProjet.RecommandationProposee;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandationProjet;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import mg.gov.mef.dsi.gpdi.service.ReponseRecommandationProjetService;
import mg.gov.mef.dsi.gpdi.util.RecommandationProjetConsignes;
import mg.gov.mef.dsi.gpdi.util.SimilariteTexte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReponseRecommandationProjetServiceImpl implements ReponseRecommandationProjetService {

    private static final Logger log = LoggerFactory.getLogger(ReponseRecommandationProjetServiceImpl.class);

    private static final int MAX_LONGUEUR_LIBELLE = 300;
    private static final int MAX_LONGUEUR_JUSTIFICATION = 500;
    private static final int MAX_LONGUEUR_PREUVE = 300;
    /** Au-dela, la nouvelle estimation est jugee aberrante et la recommandation ecartee. */
    private static final double MAX_ESTIMATION_HEURES = 500.0;

    private final ObjectMapper objectMapper;

    @Override
    public ReponseRecommandationProjet interpreter(String reponseJson, Set<Long> idsTaches, Set<Long> idsMembres,
                                                    List<String> dejaProposees) {
        JsonNode brutes = lire(reponseJson).get("recommandations");
        if (brutes == null || !brutes.isArray()) {
            throw new ReponseIaInvalideException("La réponse de l'assistant IA ne contient pas la liste de recommandations attendue.");
        }

        List<RecommandationProposee> retenues = new ArrayList<>();
        List<String> libelles = new ArrayList<>(dejaProposees == null ? Collections.<String>emptyList() : dejaProposees);
        for (JsonNode noeud : brutes) {
            if (retenues.size() >= RecommandationProjetConsignes.MAX_RECOMMANDATIONS) {
                log.warn("Recommandations au-dela du plafond de {} ignorees", RecommandationProjetConsignes.MAX_RECOMMANDATIONS);
                break;
            }
            String libelle = texte(noeud, "libelle", MAX_LONGUEUR_LIBELLE);
            if (libelle == null || estDoublon(libelle, libelles)) {
                log.info("Recommandation ecartee : libelle vide ou deja propose");
                continue;
            }
            RecommandationProposee recommandation = construire(noeud, libelle, idsTaches, idsMembres);
            if (recommandation == null) {
                log.info("Recommandation ecartee : tache, membre ou estimation non exploitable");
                continue;
            }
            libelles.add(libelle);
            retenues.add(recommandation);
        }

        // Une liste vide est un resultat valide (l'assistant peut ne detecter aucune action a proposer).
        return new ReponseRecommandationProjet(retenues);
    }

    private JsonNode lire(String reponseJson) {
        if (!StringUtils.hasText(reponseJson)) {
            throw new ReponseIaInvalideException("La réponse de l'assistant IA est vide.");
        }
        try {
            JsonNode racine = objectMapper.readTree(reponseJson);
            if (racine == null || !racine.isObject()) {
                throw new ReponseIaInvalideException("La réponse de l'assistant IA n'est pas un objet JSON.");
            }
            return racine;
        } catch (IOException e) {
            throw new ReponseIaInvalideException("La réponse de l'assistant IA n'est pas un JSON valide.", e);
        }
    }

    /** Renvoie null si la recommandation n'est pas executable telle quelle (tache, membre ou estimation invalide). */
    private RecommandationProposee construire(JsonNode noeud, String libelle, Set<Long> idsTaches, Set<Long> idsMembres) {
        String type = texte(noeud, "type", MAX_LONGUEUR_LIBELLE);
        Long idTache = identifiantParmi(noeud.get("idTache"), idsTaches);
        if (type == null || idTache == null) {
            return null;
        }
        String justification = texte(noeud, "justification", MAX_LONGUEUR_JUSTIFICATION);
        String preuve = texte(noeud, "preuve", MAX_LONGUEUR_PREUVE);

        if (TypeActionRecommandationProjet.REASSIGNER_TACHE.name().equals(type)) {
            Long idAssigne = identifiantParmi(noeud.get("idAssigne"), idsMembres);
            if (idAssigne == null) {
                return null;
            }
            return new RecommandationProposee(TypeActionRecommandationProjet.REASSIGNER_TACHE, libelle, justification, preuve,
                    idTache, idAssigne, null);
        }
        if (TypeActionRecommandationProjet.RETIRER_DU_SPRINT.name().equals(type)) {
            return new RecommandationProposee(TypeActionRecommandationProjet.RETIRER_DU_SPRINT, libelle, justification, preuve,
                    idTache, null, null);
        }
        if (TypeActionRecommandationProjet.AJUSTER_ESTIMATION.name().equals(type)) {
            Double nouvelleEstimation = estimation(noeud.get("nouvelleEstimation"));
            if (nouvelleEstimation == null) {
                return null;
            }
            return new RecommandationProposee(TypeActionRecommandationProjet.AJUSTER_ESTIMATION, libelle, justification, preuve,
                    idTache, null, nouvelleEstimation);
        }
        return null;
    }

    private Long identifiantParmi(JsonNode noeud, Set<Long> valides) {
        if (noeud == null || !noeud.isIntegralNumber() || !valides.contains(noeud.longValue())) {
            return null;
        }
        return noeud.longValue();
    }

    private Double estimation(JsonNode noeud) {
        if (noeud == null || !noeud.isNumber()) {
            return null;
        }
        double heures = noeud.doubleValue();
        return heures > 0 && heures <= MAX_ESTIMATION_HEURES ? heures : null;
    }

    private boolean estDoublon(String libelle, List<String> dejaRetenus) {
        for (String retenu : dejaRetenus) {
            if (SimilariteTexte.similaires(libelle, retenu)) {
                return true;
            }
        }
        return false;
    }

    private String texte(JsonNode parent, String champ, int longueurMax) {
        JsonNode noeud = parent.get(champ);
        if (noeud == null || !noeud.isTextual() || !StringUtils.hasText(noeud.asText())) {
            return null;
        }
        return tronquer(noeud.asText().trim(), longueurMax);
    }

    private String tronquer(String valeur, int longueurMax) {
        return valeur.length() <= longueurMax ? valeur : valeur.substring(0, longueurMax - 1) + "…";
    }
}
