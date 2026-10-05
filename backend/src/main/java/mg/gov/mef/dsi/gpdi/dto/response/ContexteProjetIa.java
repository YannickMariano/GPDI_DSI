package mg.gov.mef.dsi.gpdi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;

/**
 * Photographie d'un projet destinee a l'assistant IA : le JSON envoye au modele (et conserve dans
 * ia_analyses.contexte), plus les ensembles de reference servant a verifier que la reponse du modele
 * ne cite que des elements reels (jamais d'identifiant invente).
 */
@Getter
@AllArgsConstructor
public class ContexteProjetIa {

    /** Donnees envoyees au modele. Ne contient ni matricule, ni email, ni telephone, ni commentaire. */
    private final JsonNode contenu;

    /** Utilisateurs pouvant etre designes porteur d'un risque : responsable, chef d'equipe et membres actifs. */
    private final Set<Long> idsPorteursPossibles;

    /** Risques SOUS_CONTROLE : seuls ceux-la peuvent etre reevalues par l'IA. */
    private final Set<Long> idsRisquesReevaluables;

    /** Libelles de tous les risques deja connus (tous statuts, y compris rejetes et clos), pour eviter les doublons. */
    private final List<String> libellesRisquesConnus;
}
