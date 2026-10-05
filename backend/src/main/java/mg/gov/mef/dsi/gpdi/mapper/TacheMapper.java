package mg.gov.mef.dsi.gpdi.mapper;

import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.CommentaireResponse;
import mg.gov.mef.dsi.gpdi.dto.response.SousTacheResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TacheDetailResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TachePrecedenceResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TacheResponse;
import mg.gov.mef.dsi.gpdi.dto.response.UtilisateurAssigneResponse;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.Commentaire;
import mg.gov.mef.dsi.gpdi.entity.SousTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TachePrecedence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TacheMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateDebut", ignore = true)
    @Mapping(target = "dateFinPrevue", source = "dateEcheance")
    @Mapping(target = "dateFinReelle", ignore = true)
    @Mapping(target = "tempsPasse", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "projet", ignore = true)
    Tache toEntity(TacheRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateDebut", ignore = true)
    @Mapping(target = "dateFinPrevue", source = "dateEcheance")
    @Mapping(target = "dateFinReelle", ignore = true)
    @Mapping(target = "tempsPasse", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "projet", ignore = true)
    void updateEntity(TacheRequest request, @MappingTarget Tache tache);

    @Mapping(target = "dateEcheance", source = "dateFinPrevue")
    @Mapping(target = "idSprint", source = "sprint.id")
    @Mapping(target = "nomSprint", source = "sprint.nom")
    @Mapping(target = "idProjet", source = "projet.id")
    @Mapping(target = "assignations", ignore = true)
    TacheResponse toResponse(Tache tache);

    @Mapping(target = "dateEcheance", source = "dateFinPrevue")
    @Mapping(target = "idSprint", source = "sprint.id")
    @Mapping(target = "nomSprint", source = "sprint.nom")
    @Mapping(target = "idProjet", source = "projet.id")
    @Mapping(target = "assignations", ignore = true)
    @Mapping(target = "sousTaches", ignore = true)
    @Mapping(target = "dependances", ignore = true)
    @Mapping(target = "commentaires", ignore = true)
    TacheDetailResponse toDetailResponse(Tache tache);

    @Mapping(target = "idAffectation", source = "id")
    @Mapping(target = "idUtilisateur", source = "utilisateur.id")
    @Mapping(target = "matricule", source = "utilisateur.matricule")
    @Mapping(target = "nom", source = "utilisateur.nom")
    @Mapping(target = "prenom", source = "utilisateur.prenom")
    UtilisateurAssigneResponse toAssignationResponse(AffectationTache affectationTache);

    SousTacheResponse toSousTacheResponse(SousTache sousTache);

    @Mapping(target = "idAuteur", source = "auteur.id")
    @Mapping(target = "nomAuteur", expression = "java(commentaire.getAuteur().getPrenom() + \" \" + commentaire.getAuteur().getNom())")
    @Mapping(target = "pieceJointe", ignore = true)
    CommentaireResponse toCommentaireResponse(Commentaire commentaire);

    @Mapping(target = "idTacheSource", source = "tacheSource.id")
    @Mapping(target = "codeTacheSource", source = "tacheSource.code")
    @Mapping(target = "titreTacheSource", source = "tacheSource.titre")
    @Mapping(target = "idTacheCible", source = "tacheCible.id")
    @Mapping(target = "codeTacheCible", source = "tacheCible.code")
    @Mapping(target = "titreTacheCible", source = "tacheCible.titre")
    TachePrecedenceResponse toPrecedenceResponse(TachePrecedence tachePrecedence);
}
