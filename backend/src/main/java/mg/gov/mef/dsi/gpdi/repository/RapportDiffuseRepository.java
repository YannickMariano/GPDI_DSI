package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.RapportDiffuse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RapportDiffuseRepository extends JpaRepository<RapportDiffuse, Long> {

    List<RapportDiffuse> findAllByOrderByDateDiffusionDesc();
}
