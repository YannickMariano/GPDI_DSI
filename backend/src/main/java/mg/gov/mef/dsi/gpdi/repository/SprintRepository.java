package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    Page<Sprint> findByProjetId(Long idProjet, Pageable pageable);

    List<Sprint> findByProjetId(Long idProjet);

    List<Sprint> findByStatut(StatutSprint statut);

    List<Sprint> findByStatutAndDateDebutLessThanEqualOrderByDateDebutAsc(StatutSprint statut, LocalDate date);
}
