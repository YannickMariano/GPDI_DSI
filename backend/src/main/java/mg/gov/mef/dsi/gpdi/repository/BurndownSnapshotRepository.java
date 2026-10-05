package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.BurndownSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BurndownSnapshotRepository extends JpaRepository<BurndownSnapshot, Long> {

    List<BurndownSnapshot> findBySprintIdOrderByDateAsc(Long idSprint);

    boolean existsBySprintIdAndDate(Long idSprint, LocalDate date);
}
