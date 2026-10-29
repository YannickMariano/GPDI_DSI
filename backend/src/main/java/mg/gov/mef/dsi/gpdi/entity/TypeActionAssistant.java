package mg.gov.mef.dsi.gpdi.entity;

/**
 * Actions que l'assistant IA peut proposer. Chacune est executee par le service metier existant (memes regles,
 * meme matrice d'habilitations) et exige en plus le role Keycloak qui protege la route HTTP equivalente.
 */
public enum TypeActionAssistant {
    CREER_PROJET("GESTIONNAIRE_PROJET", false),
    MODIFIER_PROJET("GESTIONNAIRE_PROJET", false),
    DEMARRER_PROJET("GESTIONNAIRE_PROJET", false),
    SUSPENDRE_PROJET("GESTIONNAIRE_PROJET", false),
    REPRENDRE_PROJET("GESTIONNAIRE_PROJET", false),
    CLOTURER_PROJET("GESTIONNAIRE_PROJET", true),
    ARCHIVER_PROJET("GESTIONNAIRE_PROJET", true),

    CREER_SPRINT("GESTIONNAIRE_SPRINT", false),
    MODIFIER_SPRINT("GESTIONNAIRE_SPRINT", false),
    DEMARRER_SPRINT("GESTIONNAIRE_SPRINT", false),
    CLOTURER_SPRINT("GESTIONNAIRE_SPRINT", false),
    SUPPRIMER_SPRINT("GESTIONNAIRE_SPRINT", true),

    CREER_JALON("GESTIONNAIRE_JALON", false),
    MODIFIER_JALON("GESTIONNAIRE_JALON", false),
    ATTEINDRE_JALON("GESTIONNAIRE_JALON", false),
    ANNULER_JALON("GESTIONNAIRE_JALON", false),
    SUPPRIMER_JALON("GESTIONNAIRE_JALON", true),

    CREER_TACHE("GESTIONNAIRE_TACHE", false),
    MODIFIER_TACHE("GESTIONNAIRE_TACHE", false),
    AFFECTER_TACHE("GESTIONNAIRE_TACHE", false),
    DESAFFECTER_TACHE("GESTIONNAIRE_TACHE", false),
    CHANGER_STATUT_TACHE("GESTIONNAIRE_TACHE", false),
    ARCHIVER_TACHE("GESTIONNAIRE_TACHE", false),
    RESTAURER_TACHE("GESTIONNAIRE_TACHE", false),
    SUPPRIMER_TACHE("GESTIONNAIRE_TACHE", true);

    private final String roleKeycloak;
    private final boolean irreversible;

    TypeActionAssistant(String roleKeycloak, boolean irreversible) {
        this.roleKeycloak = roleKeycloak;
        this.irreversible = irreversible;
    }

    public String getRoleKeycloak() {
        return roleKeycloak;
    }

    /** Action definitive (suppression, cloture, archivage de projet) : signalee comme telle avant confirmation. */
    public boolean isIrreversible() {
        return irreversible;
    }
}
