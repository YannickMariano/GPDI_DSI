package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.response.RapportDiffuseResponse;
import mg.gov.mef.dsi.gpdi.entity.RapportDiffuse;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RapportDiffuseMapper {

    /** Le contenu (JSON stocke en texte) est renseigne par le service, qui dispose de l'ObjectMapper. */
    @Mapping(target = "idProjet", source = "projet.id")
    @Mapping(target = "nomProjet", source = "projet.titre")
    @Mapping(target = "idAuteur", source = "auteur.id")
    @Mapping(target = "nomAuteur", source = "auteur")
    @Mapping(target = "contenu", ignore = true)
    RapportDiffuseResponse toResponse(RapportDiffuse rapport);

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
