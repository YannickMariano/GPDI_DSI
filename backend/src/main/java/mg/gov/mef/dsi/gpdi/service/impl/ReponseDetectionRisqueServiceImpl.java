package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque.ReevaluationRisque;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque.RisqueDetecte;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import mg.gov.mef.dsi.gpdi.service.ReponseDetectionRisqueService;
import mg.gov.mef.dsi.gpdi.util.DetectionRisqueConsignes;
import mg.gov.mef.dsi.gpdi.util.SimilariteTexte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReponseDetectionRisqueServiceImpl implements ReponseDetectionRisqueService {

    private static final Logger log = LoggerFactory.getLogger(ReponseDetectionRisqueServiceImpl.class);

    private static final int MAX_LONGUEUR_LIBELLE = 300;
    private static final int MAX_LONGUEUR_DESCRIPTION = 2000;
    private static final int MAX_LONGUEUR_PARADE = 1000;
    private static final int MAX_LONGUEUR_SYNTHESE = 2000;
    private static final int NOTE_MIN = 1;
    private static final int NOTE_MAX = 5;

    private final ObjectMapper objectMapper;

    @Override
    public ReponseDetectionRisque interpreter(String reponseJson, ContexteProjetIa contexte) {
        JsonNode racine = lire(reponseJson);
        JsonNode nouveaux = racine.get("nouveauxRisques");
        if (nouveaux == null || !nouveaux.isArray()) {
            throw new ReponseIaInvalideException("La réponse de l'assistant IA ne contient pas la liste des risques attendue.");
        }

        List<RisqueDetecte> risques = extraireNouveauxRisques(nouveaux, contexte);
        List<ReevaluationRisque> reevaluations = extraireReevaluations(racine.get("reevaluations"), contexte);
        return new ReponseDetectionRisque(risques, reevaluations,
                texte(racine, "synthese", MAX_LONGUEUR_SYNTHESE), indiceConfiance(racine.get("indiceConfiance")));
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

    private List<RisqueDetecte> extraireNouveauxRisques(JsonNode nouveaux, ContexteProjetIa contexte) {
        List<String> libellesDejaConnus = new ArrayList<>(contexte.getLibellesRisquesConnus());
        List<RisqueDetecte> retenus = new ArrayList<>();

        for (JsonNode noeud : nouveaux) {
            if (retenus.size() >= DetectionRisqueConsignes.MAX_NOUVEAUX_RISQUES) {
                log.warn("Risques au-dela du plafond de {} ignores", DetectionRisqueConsignes.MAX_NOUVEAUX_RISQUES);
                break;
            }
            String libelle = texte(noeud, "libelle", MAX_LONGUEUR_LIBELLE);
            Integer probabilite = note(noeud.get("probabilite"));
            Integer impact = note(noeud.get("impact"));
            if (libelle == null || probabilite == null || impact == null) {
                log.warn("Risque ecarte : libelle vide ou notes hors de {} a {}", NOTE_MIN, NOTE_MAX);
                continue;
            }
            if (estDoublon(libelle, libellesDejaConnus)) {
                log.info("Risque ecarte car deja connu ou deja propose : {}", libelle);
                continue;
            }

            libellesDejaConnus.add(libelle);
            retenus.add(new RisqueDetecte(libelle, texte(noeud, "description", MAX_LONGUEUR_DESCRIPTION),
                    probabilite, impact, texte(noeud, "parade", MAX_LONGUEUR_PARADE),
                    porteurValide(noeud.get("idPorteur"), contexte.getIdsPorteursPossibles())));
        }
        return retenus;
    }

    private List<ReevaluationRisque> extraireReevaluations(JsonNode reevaluations, ContexteProjetIa contexte) {
        List<ReevaluationRisque> retenues = new ArrayList<>();
        if (reevaluations == null || !reevaluations.isArray()) {
            return retenues;
        }

        Set<Long> dejaTraites = new HashSet<>();
        for (JsonNode noeud : reevaluations) {
            JsonNode idBrut = noeud.get("idRisque");
            Long idRisque = idBrut != null && idBrut.isIntegralNumber() ? idBrut.longValue() : null;
            Integer probabilite = note(noeud.get("probabilite"));
            Integer impact = note(noeud.get("impact"));
            if (idRisque == null || !contexte.getIdsRisquesReevaluables().contains(idRisque)
                    || probabilite == null || impact == null || !dejaTraites.add(idRisque)) {
                log.warn("Reevaluation ecartee (risque non reevaluable, notes invalides ou doublon) : {}", noeud);
                continue;
            }
            retenues.add(new ReevaluationRisque(idRisque, probabilite, impact));
        }
        return retenues;
    }

    private boolean estDoublon(String libelle, List<String> libellesConnus) {
        for (String connu : libellesConnus) {
            if (SimilariteTexte.similaires(libelle, connu)) {
                return true;
            }
        }
        return false;
    }

    /** Une note doit etre un entier de 1 a 5 : hors de cet intervalle le modele s'est trompe d'echelle, on ne corrige pas a sa place. */
    private Integer note(JsonNode noeud) {
        if (noeud == null || !noeud.isIntegralNumber()) {
            return null;
        }
        long valeur = noeud.longValue();
        return valeur >= NOTE_MIN && valeur <= NOTE_MAX ? (int) valeur : null;
    }

    private Long porteurValide(JsonNode noeud, Set<Long> idsPossibles) {
        if (noeud == null || !noeud.isIntegralNumber()) {
            return null;
        }
        return idsPossibles.contains(noeud.longValue()) ? noeud.longValue() : null;
    }

    /** Les petits modeles donnent parfois des valeurs hors bornes (ex. 2.5) : elles sont ignorees plutot que stockees. */
    private BigDecimal indiceConfiance(JsonNode noeud) {
        if (noeud == null || !noeud.isNumber()) {
            return null;
        }
        BigDecimal valeur = noeud.decimalValue();
        if (valeur.compareTo(BigDecimal.ZERO) < 0 || valeur.compareTo(BigDecimal.ONE) > 0) {
            return null;
        }
        return valeur.setScale(3, RoundingMode.HALF_UP);
    }

    private String texte(JsonNode parent, String champ, int longueurMax) {
        JsonNode noeud = parent.get(champ);
        if (noeud == null || !noeud.isTextual() || !StringUtils.hasText(noeud.asText())) {
            return null;
        }
        String valeur = noeud.asText().trim();
        return valeur.length() <= longueurMax ? valeur : valeur.substring(0, longueurMax - 1) + "…";
    }
}
