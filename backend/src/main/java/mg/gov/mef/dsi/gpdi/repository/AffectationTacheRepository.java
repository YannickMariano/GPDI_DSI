package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AffectationTacheRepository extends JpaRepository<AffectationTache, Long> {

    List<AffectationTache> findByTacheId(Long idTache);

    List<AffectationTache> findByTacheIdIn(List<Long> idsTache);

    void deleteByTacheId(Long idTache);

    void deleteByUtilisateurId(Long idUtilisateur);

    boolean existsByUtilisateurId(Long idUtilisateur);

    boolean existsByTacheProjetIdAndUtilisateurId(Long idProjet, Long idUtilisateur);
}
