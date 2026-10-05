package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.StatutJalon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JalonRepository extends JpaRepository<Jalon, Long> {

    Page<Jalon> findByProjetId(Long idProjet, Pageable pageable);

    List<Jalon> findByStatutAndDateEcheanceLessThan(StatutJalon statut, LocalDate dateEcheance);
}
