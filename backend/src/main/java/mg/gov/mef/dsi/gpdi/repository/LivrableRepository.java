package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.Livrable;
import mg.gov.mef.dsi.gpdi.entity.StatutLivrable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LivrableRepository extends JpaRepository<Livrable, Long> {

    Page<Livrable> findByJalonId(Long idJalon, Pageable pageable);

    boolean existsByJalonIdAndStatut(Long idJalon, StatutLivrable statut);
}
