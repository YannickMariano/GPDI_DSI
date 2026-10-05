package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.request.SprintRequest;
import mg.gov.mef.dsi.gpdi.dto.response.SprintResponse;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SprintMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "progression", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "projet", ignore = true)
    Sprint toEntity(SprintRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "progression", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "projet", ignore = true)
    void updateEntity(SprintRequest request, @MappingTarget Sprint sprint);

    @Mapping(target = "idProjet", source = "projet.id")
    SprintResponse toResponse(Sprint sprint);
}
