package mg.gov.mef.dsi.gpdi.util;

/**
 * Regle unique de progression d'un projet, partagee par l'API projets (cartes du frontend) et le
 * contexte envoye a l'assistant IA : pourcentage arrondi de taches TERMINEE parmi toutes les taches
 * du projet, 0 si le projet n'a aucune tache. Calculee a la lecture, jamais stockee en base.
 */
public final class ProgressionProjet {

    private ProgressionProjet() {
    }

    public static int calculer(long tachesTerminees, long totalTaches) {
        if (totalTaches <= 0) {
            return 0;
        }
        return (int) Math.round(tachesTerminees * 100.0 / totalTaches);
    }
}
