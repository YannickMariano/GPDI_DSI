package mg.gov.mef.dsi.gpdi.entity;

/**
 * Actions qu'une recommandation proactive de pilotage de projet peut executer (champ "type" de
 * ia_recommandations.action_proposee, quand la recommandation ne concerne pas un risque). Distinct de
 * {@link TypeActionRecommandation} (module Risques) : les deux modules restent independants.
 */
public enum TypeActionRecommandationProjet {
    /** Confier une tache a un autre membre de l'equipe, moins charge. */
    REASSIGNER_TACHE,
    /** Sortir une tache d'un sprint en surcapacite ; elle retourne au backlog du projet. */
    RETIRER_DU_SPRINT,
    /** Corriger l'estimation d'une tache qui semble irrealiste au vu des donnees. */
    AJUSTER_ESTIMATION
}
