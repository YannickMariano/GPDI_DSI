package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.request.RoleRequest;
import mg.gov.mef.dsi.gpdi.dto.response.RoleResponse;
import mg.gov.mef.dsi.gpdi.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    Role toEntity(RoleRequest request);

    void updateEntity(RoleRequest request, @MappingTarget Role role);

    RoleResponse toResponse(Role role);
}