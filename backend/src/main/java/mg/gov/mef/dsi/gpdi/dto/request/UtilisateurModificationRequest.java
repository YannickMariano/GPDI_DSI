package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class UtilisateurModificationRequest {

    @NotBlank
    @Size(max = 50)
    private String matricule;

    @NotBlank
    @Size(max = 100)
    private String nom;

    @NotBlank
    @Size(max = 100)
    private String prenom;

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String telephone;

    /**
     * Optionnel : si absent ou vide, le mot de passe existant est conserve.
     */
    @Size(min = 6, max = 100)
    private String motDePasse;

    @Size(max = 150)
    private String specialite;

    @NotNull
    private Long idRole;
}
