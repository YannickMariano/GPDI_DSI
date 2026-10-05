package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabilitationRepository extends JpaRepository<Habilitation, Long> {

    List<Habilitation> findAllByOrderByIdAsc();

    Optional<Habilitation> findByFonctionAndRoleNom(String fonction, String roleNom);
}
