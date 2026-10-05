package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.PieceJointeCommentaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PieceJointeCommentaireRepository extends JpaRepository<PieceJointeCommentaire, Long> {

    Optional<PieceJointeCommentaire> findByCommentaireId(Long idCommentaire);

    List<PieceJointeCommentaire> findByCommentaireIdIn(List<Long> idsCommentaire);
}
