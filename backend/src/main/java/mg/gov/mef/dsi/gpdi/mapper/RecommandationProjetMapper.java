package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.response.RecommandationProjetResponse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecommandationProjetMapper {

    @Mapping(target = "type", expression = "java(recommandation.getActionProposee().path(\"type\").asText(null))")
    @Mapping(target = "preuve", expression = "java(recommandation.getActionProposee().path(\"preuve\").asText(null))")
    @Mapping(target = "statut", expression = "java(recommandation.getStatut().name())")
    @Mapping(target = "resultat", expression = "java(recommandation.getResultatApplication() != null ? recommandation.getResultatApplication().path(\"resultat\").asText(null) : null)")
    RecommandationProjetResponse toResponse(IaRecommandation recommandation);
}
