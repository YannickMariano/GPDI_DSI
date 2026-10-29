package mg.gov.mef.dsi.gpdi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(RoleIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererRoleIntrouvable(RoleIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(UtilisateurIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererUtilisateurIntrouvable(UtilisateurIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(UtilisateurAssigneATachesException.class)
        public ResponseEntity<ErreurResponse> gererUtilisateurAssigneATaches(UtilisateurAssigneATachesException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(CompteSuspenduException.class)
        public ResponseEntity<ErreurResponse> gererCompteSuspendu(CompteSuspenduException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(PieceJointeInvalideException.class)
        public ResponseEntity<ErreurResponse> gererPieceJointeInvalide(PieceJointeInvalideException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(PieceJointeIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererPieceJointeIntrouvable(PieceJointeIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ErreurResponse> gererTailleMaxDepassee(MaxUploadSizeExceededException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse("Le fichier depasse la taille maximale autorisee (10 Mo)."));
        }

        @ExceptionHandler(EquipeIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererEquipeIntrouvable(EquipeIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(EquipeRattacheeProjetException.class)
        public ResponseEntity<ErreurResponse> gererEquipeRattacheeProjet(EquipeRattacheeProjetException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(UtilisateurNonEligibleException.class)
        public ResponseEntity<ErreurResponse> gererUtilisateurNonEligible(UtilisateurNonEligibleException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(ProjetIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererProjetIntrouvable(ProjetIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(StatutProjetInvalideException.class)
        public ResponseEntity<ErreurResponse> gererStatutProjetInvalide(StatutProjetInvalideException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(SprintIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererSprintIntrouvable(SprintIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(SprintChevauchementException.class)
        public ResponseEntity<ErreurResponse> gererSprintChevauchement(SprintChevauchementException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(DateHorsPeriodeProjetException.class)
        public ResponseEntity<ErreurResponse> gererDateHorsPeriodeProjet(DateHorsPeriodeProjetException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(StatutSprintInvalideException.class)
        public ResponseEntity<ErreurResponse> gererStatutSprintInvalide(StatutSprintInvalideException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(SprintPrecedentNonClotureException.class)
        public ResponseEntity<ErreurResponse> gererSprintPrecedentNonCloture(SprintPrecedentNonClotureException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(JalonIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererJalonIntrouvable(JalonIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(LivrableIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererLivrableIntrouvable(LivrableIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(StatutJalonInvalideException.class)
        public ResponseEntity<ErreurResponse> gererStatutJalonInvalide(StatutJalonInvalideException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(StatutLivrableInvalideException.class)
        public ResponseEntity<ErreurResponse> gererStatutLivrableInvalide(StatutLivrableInvalideException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(JalonAnnuleException.class)
        public ResponseEntity<ErreurResponse> gererJalonAnnule(JalonAnnuleException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(ClotureProjetImpossibleException.class)
        public ResponseEntity<ErreurResponse> gererClotureProjetImpossible(ClotureProjetImpossibleException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(ProjetNonDemarreException.class)
        public ResponseEntity<ErreurResponse> gererProjetNonDemarre(ProjetNonDemarreException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(LivrableEnAttenteException.class)
        public ResponseEntity<ErreurResponse> gererLivrableEnAttente(LivrableEnAttenteException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(DateLivraisonInvalideException.class)
        public ResponseEntity<ErreurResponse> gererDateLivraisonInvalide(DateLivraisonInvalideException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(DateFinTacheInvalideException.class)
        public ResponseEntity<ErreurResponse> gererDateFinTacheInvalide(DateFinTacheInvalideException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(TacheIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererTacheIntrouvable(TacheIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(SousTacheIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererSousTacheIntrouvable(SousTacheIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(UtilisateurNonMembreEquipeException.class)
        public ResponseEntity<ErreurResponse> gererUtilisateurNonMembreEquipe(UtilisateurNonMembreEquipeException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(SprintProjetIncoherentException.class)
        public ResponseEntity<ErreurResponse> gererSprintProjetIncoherent(SprintProjetIncoherentException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(TachePrecedenceInvalideException.class)
        public ResponseEntity<ErreurResponse> gererTachePrecedenceInvalide(TachePrecedenceInvalideException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(TachePrecedenceDejaExistanteException.class)
        public ResponseEntity<ErreurResponse> gererTachePrecedenceDejaExistante(TachePrecedenceDejaExistanteException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(TachePrecedenceIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererTachePrecedenceIntrouvable(TachePrecedenceIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(NotificationIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererNotificationIntrouvable(NotificationIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(PartageClientIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererPartageClientIntrouvable(PartageClientIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(PartageDejaActifException.class)
        public ResponseEntity<ErreurResponse> gererPartageDejaActif(PartageDejaActifException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(StatutPartageInvalideException.class)
        public ResponseEntity<ErreurResponse> gererStatutPartageInvalide(StatutPartageInvalideException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(UtilisateurCourantIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererUtilisateurCourantIntrouvable(UtilisateurCourantIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(AccesPointsInformationRefuseException.class)
        public ResponseEntity<ErreurResponse> gererAccesPointsInformationRefuse(AccesPointsInformationRefuseException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(AccesAssistantIaRefuseException.class)
        public ResponseEntity<ErreurResponse> gererAccesAssistantIaRefuse(AccesAssistantIaRefuseException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(AccesAssistantIaGlobalRefuseException.class)
        public ResponseEntity<ErreurResponse> gererAccesAssistantIaGlobalRefuse(AccesAssistantIaGlobalRefuseException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(AccesProjetRefuseException.class)
        public ResponseEntity<ErreurResponse> gererAccesProjetRefuse(AccesProjetRefuseException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(HabilitationRefuseeException.class)
        public ResponseEntity<ErreurResponse> gererHabilitationRefusee(HabilitationRefuseeException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(HabilitationInvalideException.class)
        public ResponseEntity<ErreurResponse> gererHabilitationInvalide(HabilitationInvalideException exception) {
                return ResponseEntity.badRequest()
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(AccesRecommandationsProjetRefuseException.class)
        public ResponseEntity<ErreurResponse> gererAccesRecommandationsProjetRefuse(AccesRecommandationsProjetRefuseException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(RecommandationProjetIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererRecommandationProjetIntrouvable(RecommandationProjetIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(StatutRecommandationProjetInvalideException.class)
        public ResponseEntity<ErreurResponse> gererStatutRecommandationProjetInvalide(StatutRecommandationProjetInvalideException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(AnalyseRecommandationsDejaEnCoursException.class)
        public ResponseEntity<ErreurResponse> gererAnalyseRecommandationsDejaEnCours(AnalyseRecommandationsDejaEnCoursException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(KeycloakIndisponibleException.class)
        public ResponseEntity<ErreurResponse> gererKeycloakIndisponible(KeycloakIndisponibleException exception) {
                log.error("Provisioning Keycloak en echec", exception);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(AnalyseDejaEnCoursException.class)
        public ResponseEntity<ErreurResponse> gererAnalyseDejaEnCours(AnalyseDejaEnCoursException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(RisqueIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererRisqueIntrouvable(RisqueIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(StatutRisqueInvalideException.class)
        public ResponseEntity<ErreurResponse> gererStatutRisqueInvalide(StatutRisqueInvalideException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(SolutionRisqueIntrouvableException.class)
        public ResponseEntity<ErreurResponse> gererSolutionRisqueIntrouvable(SolutionRisqueIntrouvableException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(StatutRecommandationInvalideException.class)
        public ResponseEntity<ErreurResponse> gererStatutRecommandationInvalide(StatutRecommandationInvalideException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(RecommandationsConflictuellesException.class)
        public ResponseEntity<ErreurResponse> gererRecommandationsConflictuelles(RecommandationsConflictuellesException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(SuggestionDejaEnCoursException.class)
        public ResponseEntity<ErreurResponse> gererSuggestionDejaEnCours(SuggestionDejaEnCoursException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(OllamaIndisponibleException.class)
        public ResponseEntity<ErreurResponse> gererOllamaIndisponible(OllamaIndisponibleException exception) {
                log.error("Assistant IA (Ollama) indisponible", exception);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new ErreurResponse(exception.getMessage()));
        }

        @ExceptionHandler(ReponseIaInvalideException.class)
        public ResponseEntity<ErreurResponse> gererReponseIaInvalide(ReponseIaInvalideException exception) {
                log.error("Reponse de l'assistant IA inexploitable", exception);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ErreurResponse(exception.getMessage() + " Veuillez réessayer."));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErreurResponse> gererValidation(MethodArgumentNotValidException exception) {
                String message = exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + " : " + error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                return ResponseEntity.badRequest().body(new ErreurResponse(message));
        }

        /**
         * Filet de sécurité pour toute exception non prévue par un handler dédié ci-dessus :
         * le detail technique est journalisé cote serveur, jamais renvoye tel quel au client.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErreurResponse> gererErreurInattendue(Exception exception) {
                log.error("Erreur interne non geree", exception);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErreurResponse("Erreur technique inattendue. Contactez l'administrateur si le problème persiste."));
        }
}