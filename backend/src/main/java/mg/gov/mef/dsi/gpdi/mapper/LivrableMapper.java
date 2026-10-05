package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.request.LivrableRequest;
import mg.gov.mef.dsi.gpdi.dto.response.LivrableResponse;
import mg.gov.mef.dsi.gpdi.entity.Livrable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LivrableMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateValidation", ignore = true)
    @Mapping(target = "dateRejet", ignore = true)
    @Mapping(target = "motifRejet", ignore = true)
    @Mapping(target = "jalon", ignore = true)
    Livrable toEntity(LivrableRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateValidation", ignore = true)
    @Mapping(target = "dateRejet", ignore = true)
    @Mapping(target = "motifRejet", ignore = true)
    @Mapping(target = "jalon", ignore = true)
    void updateEntity(LivrableRequest request, @MappingTarget Livrable livrable);

    @Mapping(target = "idJalon", source = "jalon.id")
    LivrableResponse toResponse(Livrable livrable);
}
