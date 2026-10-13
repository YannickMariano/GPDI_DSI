package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RisqueRepository extends JpaRepository<Risque, Long> {

    List<Risque> findByProjetIdOrderByIdAsc(Long idProjet);

    List<Risque> findByProjetIdAndStatut(Long idProjet, StatutRisque statut);

    /** A appeler dans une transaction : verrouille le risque pour qu'une seule demande le traite (appliquer ou rejeter) a la fois. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Risque r where r.id = :id")
    Optional<Risque> findByIdForUpdate(@Param("id") Long id);
}
