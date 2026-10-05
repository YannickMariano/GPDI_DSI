package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.PartageClient;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PartageClientRepository extends JpaRepository<PartageClient, Long> {

    boolean existsByClientIdAndProjetIdAndStatut(Long idUtilisateur, Long idProjet, StatutPartage statut);

    List<PartageClient> findByStatutAndDateEcheanceLessThan(StatutPartage statut, LocalDate dateEcheance);
}
