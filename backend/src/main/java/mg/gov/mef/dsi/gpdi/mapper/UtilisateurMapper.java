package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurCreationRequest;
import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurModificationRequest;
import mg.gov.mef.dsi.gpdi.dto.response.UtilisateurResponse;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "motDePasse", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "actif", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    Utilisateur toEntity(UtilisateurCreationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "motDePasse", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "actif", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    void updateEntity(UtilisateurModificationRequest request, @MappingTarget Utilisateur utilisateur);

    @Mapping(target = "idRole", source = "role.id")
    @Mapping(target = "nomRole", source = "role.nom")
    UtilisateurResponse toResponse(Utilisateur utilisateur);
}
