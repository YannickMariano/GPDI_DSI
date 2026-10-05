package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.SousTache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SousTacheRepository extends JpaRepository<SousTache, Long> {

    List<SousTache> findByTacheId(Long idTache);

    void deleteByTacheId(Long idTache);
}
