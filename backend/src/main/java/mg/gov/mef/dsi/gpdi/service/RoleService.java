package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.RoleRequest;
import mg.gov.mef.dsi.gpdi.dto.response.RoleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleService {

    RoleResponse creer(RoleRequest request);

    Page<RoleResponse> rechercher(Pageable pageable);

    RoleResponse obtenir(Long id);

    RoleResponse modifier(Long id, RoleRequest request);

    void supprimer(Long id);
}