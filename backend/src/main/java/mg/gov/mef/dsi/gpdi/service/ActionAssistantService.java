package mg.gov.mef.dsi.gpdi.service;

import com.fasterxml.jackson.databind.JsonNode;
import mg.gov.mef.dsi.gpdi.dto.response.ActionAssistantResponse;
import mg.gov.mef.dsi.gpdi.entity.ActionAssistant;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;

import java.util.List;
import java.util.Map;

public interface ActionAssistantService {

    /**
     * Verifie et prepare, sans rien executer, l'action decrite par l'assistant IA : chaque designation libre
     * (projet, tache, sprint, jalon, personne) est resolue parmi les projets dont le demandeur est responsable.
     * Retourne null si le modele n'a propose aucune action ; ActionAssistantImpossibleException si l'action ne
     * peut pas etre preparee (designation introuvable ou ambigue, champ obligatoire manquant). L'action
     * retournee n'est pas encore enregistree (l'appelant la rattache a son echange).
     */
    ActionAssistant preparer(JsonNode actionModele, Utilisateur demandeur);

    /** Execute l'action via le service metier correspondant (memes regles que l'ecran equivalent). */
    ActionAssistantResponse confirmer(Long idAction);

    ActionAssistantResponse annuler(Long idAction);

    ActionAssistantResponse versResponse(ActionAssistant action);

    /** Actions rattachees aux echanges donnes, indexees par identifiant d'echange (analyse). */
    Map<Long, ActionAssistantResponse> parAnalyse(List<Long> idsAnalyses);
}
