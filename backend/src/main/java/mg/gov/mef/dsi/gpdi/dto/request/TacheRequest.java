package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TacheRequest {

    @NotBlank
    @Size(max = 200)
    private String titre;

    @Size(max = 2000)
    private String description;

    @NotNull
    private Priorite priorite;

    private Double tempsEstime;

    /**
     * Membres de l'equipe du projet assignes a la tache. Peut etre vide (tache non assignee).
     */
    private List<Long> idsUtilisateursAssignes;

    /**
     * Optionnel : si absent, la tache va dans le backlog du projet.
     */
    private Long idSprint;

    /**
     * Optionnel : si absent, le statut courant de la tache est conserve (creation : A_FAIRE).
     * Le passage a EN_COURS demarre automatiquement le chronometre (demarreeLe) s'il ne l'etait
     * pas deja ; le passage a TERMINEE le fige dans tempsPasse et l'arrete.
     */
    private StatutTache statut;

    private LocalDate dateEcheance;

    /**
     * Optionnel : saisie manuelle du temps passe (heures), remplace la valeur courante si fournie.
     * Si absent, le temps passe courant est conserve (hors arret automatique du chronometre).
     */
    private Double tempsPasse;
}
