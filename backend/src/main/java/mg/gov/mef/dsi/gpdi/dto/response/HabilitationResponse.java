package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/** Une ligne de la matrice d'habilitations : une fonction, son module, et son niveau d'acces par role metier. */
@Getter
@Setter
@NoArgsConstructor
public class HabilitationResponse {

    private String module;

    private String fonction;

    /** Cle = valeur de Role.nom (ex. "chef_projet"), valeur = "ecriture" | "lecture" | "aucun". */
    private Map<String, String> acces;
}
