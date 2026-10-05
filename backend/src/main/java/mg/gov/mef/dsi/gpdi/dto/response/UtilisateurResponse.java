package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UtilisateurResponse {

    private Long id;

    private String matricule;

    private String nom;

    private String prenom;

    private String email;

    private String telephone;

    private String specialite;

    private Boolean actif;

    private LocalDateTime dateCreation;

    private Long idRole;

    private String nomRole;
}
