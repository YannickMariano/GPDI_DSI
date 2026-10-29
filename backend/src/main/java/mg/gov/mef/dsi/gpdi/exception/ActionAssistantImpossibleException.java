package mg.gov.mef.dsi.gpdi.exception;

/**
 * L'action demandee a l'assistant IA ne peut pas etre preparee (projet, tache, personne ou date introuvable ou
 * ambigu, champ obligatoire manquant). Le message, destine a l'utilisateur, dit quoi preciser.
 */
public class ActionAssistantImpossibleException extends RuntimeException {

    public ActionAssistantImpossibleException(String message) {
        super(message);
    }
}
