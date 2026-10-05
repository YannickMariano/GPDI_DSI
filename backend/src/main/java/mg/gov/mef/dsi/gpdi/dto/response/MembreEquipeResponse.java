package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MembreEquipeResponse {

    private Long id;

    private Long idUtilisateur;

    private String matricule;

    private String nom;

    private String prenom;

    private Boolean actif;
}
