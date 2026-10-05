package mg.gov.mef.dsi.gpdi.repository;

import mg.gov.mef.dsi.gpdi.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByNom(String nom);
}