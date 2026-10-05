package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UtilisateurAssigneResponse {

    private Long idAffectation;

    private Long idUtilisateur;

    private String matricule;

    private String nom;

    private String prenom;

    private LocalDateTime dateAffectation;
}
