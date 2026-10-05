package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.response.RisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RisqueMapper {

    @Mapping(target = "code", ignore = true)
    @Mapping(target = "parade", source = "mesureMitigation")
    @Mapping(target = "analyse", source = "description")
    @Mapping(target = "dateDetection", source = "dateCreation")
    @Mapping(target = "idPorteur", source = "porteur.id")
    @Mapping(target = "porteur", source = "porteur")
    @Mapping(target = "solutions", ignore = true)
    RisqueResponse toResponse(Risque risque);

    default String nomComplet(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        String prenom = utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom();
        String nom = utilisateur.getNom() == null ? "" : utilisateur.getNom();
        String complet = (prenom + " " + nom).trim();
        return complet.isEmpty() ? null : complet;
    }
}
