package mg.gov.mef.dsi.gpdi.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Comparaison de libelles courts pour detecter les doublons reformules (ex. « Retard du referentiel »
 * et « Retard de livraison du referentiel »). Volontairement simple : elle rattrape les
 * reformulations proches, pas les paraphrases qui ne partagent aucun mot significatif.
 */
public final class SimilariteTexte {

    private static final double SEUIL_JACCARD = 0.6;
    private static final double SEUIL_INCLUSION = 0.8;
    private static final int MIN_MOTS_COMMUNS_INCLUSION = 3;
    private static final int LONGUEUR_MIN_MOT = 3;

    private static final Set<String> MOTS_VIDES = new HashSet<>(Arrays.asList(
            "les", "des", "une", "aux", "dans", "pour", "par", "sur", "avec", "sans", "ces", "ses", "leur",
            "leurs", "est", "sont", "que", "qui", "cette", "entre", "vers", "chez", "plus", "tres"));

    private SimilariteTexte() {
    }

    public static Set<String> jetons(String texte) {
        Set<String> jetons = new HashSet<>();
        if (texte == null) {
            return jetons;
        }
        String normalise = Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        for (String mot : normalise.split(" ")) {
            if (mot.length() >= LONGUEUR_MIN_MOT && !MOTS_VIDES.contains(mot)) {
                jetons.add(mot);
            }
        }
        return jetons;
    }

    public static boolean similaires(String a, String b) {
        Set<String> jetonsA = jetons(a);
        Set<String> jetonsB = jetons(b);
        if (jetonsA.isEmpty() || jetonsB.isEmpty()) {
            return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
        }

        Set<String> communs = new HashSet<>(jetonsA);
        communs.retainAll(jetonsB);
        Set<String> union = new HashSet<>(jetonsA);
        union.addAll(jetonsB);

        double jaccard = (double) communs.size() / union.size();
        double inclusion = (double) communs.size() / Math.min(jetonsA.size(), jetonsB.size());
        return jaccard >= SEUIL_JACCARD
                || (communs.size() >= MIN_MOTS_COMMUNS_INCLUSION && inclusion >= SEUIL_INCLUSION);
    }
}
