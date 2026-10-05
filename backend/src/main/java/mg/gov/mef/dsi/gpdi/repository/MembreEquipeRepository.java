package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembreEquipeRepository extends JpaRepository<MembreEquipe, Long> {

    List<MembreEquipe> findByEquipeId(Long idEquipe);

    List<MembreEquipe> findByEquipeIdIn(List<Long> idsEquipe);

    boolean existsByEquipeIdAndUtilisateurId(Long idEquipe, Long idUtilisateur);

    void deleteByEquipeId(Long idEquipe);
}
