package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.response.PointInformationResponse;
import mg.gov.mef.dsi.gpdi.entity.PointInformation;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PointInformationMapper {

    @Mapping(target = "idAuteur", source = "auteur.id")
    @Mapping(target = "nomAuteur", source = "auteur")
    PointInformationResponse toResponse(PointInformation point);

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
