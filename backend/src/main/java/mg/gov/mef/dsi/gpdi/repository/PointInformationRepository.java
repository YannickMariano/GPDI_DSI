package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.PointInformation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointInformationRepository extends JpaRepository<PointInformation, Long> {

    Page<PointInformation> findByProjetIdOrderByDateCreationDesc(Long idProjet, Pageable pageable);
}
