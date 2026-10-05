package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.request.EquipeRequest;
import mg.gov.mef.dsi.gpdi.dto.response.EquipeResponse;
import mg.gov.mef.dsi.gpdi.dto.response.MembreEquipeResponse;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EquipeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "chefEquipe", ignore = true)
    Equipe toEntity(EquipeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "chefEquipe", ignore = true)
    void updateEntity(EquipeRequest request, @MappingTarget Equipe equipe);

    @Mapping(target = "idChefEquipe", source = "chefEquipe.id")
    @Mapping(target = "nomChefEquipe", expression = "java(equipe.getChefEquipe().getPrenom() + \" \" + equipe.getChefEquipe().getNom())")
    @Mapping(target = "membres", ignore = true)
    EquipeResponse toResponse(Equipe equipe);

    @Mapping(target = "idUtilisateur", source = "utilisateur.id")
    @Mapping(target = "matricule", source = "utilisateur.matricule")
    @Mapping(target = "nom", source = "utilisateur.nom")
    @Mapping(target = "prenom", source = "utilisateur.prenom")
    MembreEquipeResponse toMembreResponse(MembreEquipe membreEquipe);
}
