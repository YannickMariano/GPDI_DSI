package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.TachePrecedence;
import mg.gov.mef.dsi.gpdi.entity.TachePrecedenceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TachePrecedenceRepository extends JpaRepository<TachePrecedence, TachePrecedenceId> {

    List<TachePrecedence> findByTacheSourceIdOrTacheCibleId(Long idTacheSource, Long idTacheCible);

    List<TachePrecedence> findByTacheCibleId(Long idTacheCible);

    boolean existsByTacheSourceIdAndTacheCibleId(Long idTacheSource, Long idTacheCible);

    void deleteByTacheSourceIdOrTacheCibleId(Long idTacheSource, Long idTacheCible);
}
