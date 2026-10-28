package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.request.ProjetRequest;
import mg.gov.mef.dsi.gpdi.dto.response.ProjetResponse;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProjetMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "codeReference", ignore = true)
    @Mapping(target = "dateCloture", ignore = true)
    @Mapping(target = "progression", ignore = true)
    @Mapping(target = "archive", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    @Mapping(target = "equipe", ignore = true)
    Projet toEntity(ProjetRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "codeReference", ignore = true)
    @Mapping(target = "dateCloture", ignore = true)
    @Mapping(target = "progression", ignore = true)
    @Mapping(target = "archive", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    @Mapping(target = "equipe", ignore = true)
    // Le statut n'est jamais mappe ici : ProjetServiceImpl.modifier() le gere lui-meme (ne le change que
    // si explicitement fourni), sinon MapStruct l'ecraserait avec le null habituel d'un formulaire d'edition.
    @Mapping(target = "statut", ignore = true)
    void updateEntity(ProjetRequest request, @MappingTarget Projet projet);

    @Mapping(target = "idResponsable", source = "responsable.id")
    @Mapping(target = "nomResponsable", expression = "java(projet.getResponsable().getPrenom() + \" \" + projet.getResponsable().getNom())")
    @Mapping(target = "direction", source = "responsable.specialite")
    @Mapping(target = "idEquipe", source = "equipe.id")
    @Mapping(target = "nomEquipe", source = "equipe.nom")
    @Mapping(target = "sante", ignore = true)
    ProjetResponse toResponse(Projet projet);
}
