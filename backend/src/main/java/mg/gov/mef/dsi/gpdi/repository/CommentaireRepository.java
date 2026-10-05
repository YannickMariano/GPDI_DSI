package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    List<Commentaire> findByTacheIdOrderByDateCreationAsc(Long idTache);

    void deleteByTacheId(Long idTache);
}
