package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ProjetRepository extends JpaRepository<Projet, Long> {

    List<Projet> findByStatutAndDateDebutLessThanEqual(StatutProjet statut, LocalDate dateDebut);

    boolean existsByEquipeId(Long idEquipe);
}
