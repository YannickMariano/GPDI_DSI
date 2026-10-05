package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseSuggestionRisque;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseSuggestionRisque.SolutionProposee;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandation;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import mg.gov.mef.dsi.gpdi.service.ReponseSuggestionRisqueService;
import mg.gov.mef.dsi.gpdi.util.SimilariteTexte;
import mg.gov.mef.dsi.gpdi.util.SuggestionRisqueConsignes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReponseSuggestionRisqueServiceImpl implements ReponseSuggestionRisqueService {

    private static final Logger log = LoggerFactory.getLogger(ReponseSuggestionRisqueServiceImpl.class);

    private static final int MAX_LONGUEUR_LIBELLE = 300;
    private static final int MAX_LONGUEUR_JUSTIFICATION = 1000;
    private static final int MAX_LONGUEUR_GAIN = 200;
    /** Meme limite que le titre d'une tache (TacheRequest). */
    private static final int MAX_LONGUEUR_TITRE_TACHE = 200;
    private static final int MAX_LONGUEUR_DESCRIPTION_TACHE = 2000;
    /** Au-dela, l'estimation est jugee aberrante et ignoree. */
    private static final double MAX_TEMPS_ESTIME_HEURES = 500.0;

    private final ObjectMapper objectMapper;

    @Override
    public ReponseSuggestionRisque interpreter(String reponseJson, Set<Long> idsMembres, Set<Long> idsTachesReaffectables) {
        JsonNode brutes = lire(reponseJson).get("solutions");
        if (brutes == null || !brutes.isArray()) {
            throw new ReponseIaInvalideException("La réponse de l'assistant IA ne contient pas la liste de solutions attendue.");
        }

        List<SolutionProposee> retenues = new ArrayList<>();
        List<String> libelles = new ArrayList<>();
        for (JsonNode noeud : brutes) {
            if (retenues.size() >= SuggestionRisqueConsignes.MAX_SOLUTIONS) {
                log.warn("Solutions au-dela du plafond de {} ignorees", SuggestionRisqueConsignes.MAX_SOLUTIONS);
                break;
            }
            String libelle = texte(noeud, "libelle", MAX_LONGUEUR_LIBELLE);
            if (libelle == null || estDoublon(libelle, libelles)) {
                log.warn("Solution ecartee : libelle vide ou deja propose");
                continue;
            }
            libelles.add(libelle);
            retenues.add(construire(noeud, libelle, idsMembres, idsTachesReaffectables));
        }

        if (retenues.isEmpty()) {
            throw new ReponseIaInvalideException("L'assistant IA n'a proposé aucune solution exploitable.");
        }
        return new ReponseSuggestionRisque(retenues);
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

    /**
     * Une reaffectation n'est gardee que si la tache et le membre sont reels. Sinon (ou si le type est
     * inconnu), la solution n'est pas executable telle quelle : elle devient une tache de suivi, dont
     * le titre est le libelle de la solution, pour qu'elle ne soit pas perdue.
     */
    private SolutionProposee construire(JsonNode noeud, String libelle, Set<Long> idsMembres, Set<Long> idsTachesReaffectables) {
        String justification = texte(noeud, "justification", MAX_LONGUEUR_JUSTIFICATION);
        String gain = texte(noeud, "gain", MAX_LONGUEUR_GAIN);
        Long idAssigne = identifiantParmi(noeud.get("idAssigne"), idsMembres);

        if (TypeActionRecommandation.REASSIGNER_TACHE.name().equals(texte(noeud, "typeAction", MAX_LONGUEUR_LIBELLE))) {
            Long idTache = identifiantParmi(noeud.get("idTache"), idsTachesReaffectables);
            if (idTache != null && idAssigne != null) {
                return new SolutionProposee(libelle, justification, gain, TypeActionRecommandation.REASSIGNER_TACHE,
                        idTache, idAssigne, null, null, null);
            }
            log.info("Reaffectation non executable (tache ou membre inconnu) : transformee en tache de suivi");
        }

        String titre = texte(noeud, "titreTache", MAX_LONGUEUR_TITRE_TACHE);
        String description = texte(noeud, "descriptionTache", MAX_LONGUEUR_DESCRIPTION_TACHE);
        return new SolutionProposee(libelle, justification, gain, TypeActionRecommandation.CREER_TACHE, null, idAssigne,
                titre != null ? titre : tronquer(libelle, MAX_LONGUEUR_TITRE_TACHE),
                description != null ? description : justification, tempsEstime(noeud.get("tempsEstime")));
    }

    private Long identifiantParmi(JsonNode noeud, Set<Long> valides) {
        if (noeud == null || !noeud.isIntegralNumber() || !valides.contains(noeud.longValue())) {
            return null;
        }
        return noeud.longValue();
    }

    private Double tempsEstime(JsonNode noeud) {
        if (noeud == null || !noeud.isNumber()) {
            return null;
        }
        double heures = noeud.doubleValue();
        return heures > 0 && heures <= MAX_TEMPS_ESTIME_HEURES ? heures : null;
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
