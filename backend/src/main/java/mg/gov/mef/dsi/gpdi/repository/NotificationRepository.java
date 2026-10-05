package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByDestinataireIdOrderByDateCreationDesc(Long idUtilisateur, Pageable pageable);

    Optional<Notification> findByIdAndDestinataireId(Long id, Long idUtilisateur);

    boolean existsByDestinataireIdAndCle(Long idUtilisateur, String cle);

    @Modifying
    @Query("update Notification n set n.lu = true where n.destinataire.id = :idUtilisateur and n.lu = false")
    int marquerToutesCommeLues(@Param("idUtilisateur") Long idUtilisateur);
}
