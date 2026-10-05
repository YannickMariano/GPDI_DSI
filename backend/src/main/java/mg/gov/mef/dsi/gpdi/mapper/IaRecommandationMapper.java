package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.response.SolutionRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IaRecommandationMapper {

    @Mapping(target = "gain", expression = "java(recommandation.getActionProposee().path(\"gain\").asText(null))")
    @Mapping(target = "typeAction", expression = "java(recommandation.getActionProposee().path(\"type\").asText(null))")
    @Mapping(target = "statut", expression = "java(recommandation.getStatut().name())")
    @Mapping(target = "codeTache", expression = "java(recommandation.getResultatApplication() != null ? recommandation.getResultatApplication().path(\"codeTache\").asText(null) : null)")
    @Mapping(target = "motifRejet", source = "motifRejet")
    SolutionRisqueResponse toSolutionResponse(IaRecommandation recommandation);
}
