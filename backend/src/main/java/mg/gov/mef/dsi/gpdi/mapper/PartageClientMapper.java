package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.response.PartageClientResponse;
import mg.gov.mef.dsi.gpdi.entity.PartageClient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PartageClientMapper {

    @Mapping(target = "idUtilisateur", source = "client.id")
    @Mapping(target = "nomClient", expression = "java(partageClient.getClient().getPrenom() + \" \" + partageClient.getClient().getNom())")
    @Mapping(target = "idProjet", source = "projet.id")
    @Mapping(target = "titreProjet", source = "projet.titre")
    PartageClientResponse toResponse(PartageClient partageClient);
}
