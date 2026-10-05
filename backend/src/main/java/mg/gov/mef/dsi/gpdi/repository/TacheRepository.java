package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.Tache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import mg.gov.mef.dsi.gpdi.entity.StatutTache;

import java.time.LocalDate;
import java.util.List;

public interface TacheRepository extends JpaRepository<Tache, Long> {

    Page<Tache> findByProjetId(Long idProjet, Pageable pageable);

    List<Tache> findBySprintId(Long idSprint);

    List<Tache> findByDateFinPrevueLessThanEqualAndStatutNot(LocalDate limite, StatutTache statut);
}
