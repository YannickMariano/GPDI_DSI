package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.request.JalonRequest;
import mg.gov.mef.dsi.gpdi.dto.response.JalonResponse;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface JalonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateValidation", ignore = true)
    @Mapping(target = "projet", ignore = true)
    Jalon toEntity(JalonRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateValidation", ignore = true)
    @Mapping(target = "projet", ignore = true)
    void updateEntity(JalonRequest request, @MappingTarget Jalon jalon);

    @Mapping(target = "idProjet", source = "projet.id")
    JalonResponse toResponse(Jalon jalon);
}
