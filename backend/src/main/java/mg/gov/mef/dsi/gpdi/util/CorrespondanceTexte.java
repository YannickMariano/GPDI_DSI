package mg.gov.mef.dsi.gpdi.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Retrouve l'element designe par un nom libre (« Koto », « sprint 3 », « refonte portail ») parmi des
 * candidats, sans tenir compte des accents, de la casse ni de la ponctuation. Retourne tous les meilleurs
 * candidats : plusieurs resultats signifient une designation ambigue, a faire preciser a l'utilisateur.
 */
public final class CorrespondanceTexte {

    private static final int SCORE_EGAL = 3;
    private static final int SCORE_MOTS = 2;
    private static final int SCORE_INCLUSION = 1;
    private static final int LONGUEUR_MIN_INCLUSION = 3;

    private CorrespondanceTexte() {
    }

    public static String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        return Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    public static <T> List<T> meilleurs(String recherche, List<T> candidats, Function<T, String> libelle) {
        String cible = normaliser(recherche);
        List<T> retenus = new ArrayList<>();
        if (cible.isEmpty()) {
            return retenus;
        }
        int meilleurScore = 0;
        for (T candidat : candidats) {
            int score = score(cible, normaliser(libelle.apply(candidat)));
            if (score > meilleurScore) {
                meilleurScore = score;
                retenus.clear();
            }
            if (score > 0 && score == meilleurScore) {
                retenus.add(candidat);
            }
        }
        return retenus;
    }

    /** « a koto » correspond a « andry koto » : chaque mot recherche est le debut d'un mot du candidat. */
    private static int score(String cible, String candidat) {
        if (candidat.isEmpty()) {
            return 0;
        }
        if (cible.equals(candidat)) {
            return SCORE_EGAL;
        }
        List<String> motsCandidat = Arrays.asList(candidat.split(" "));
        List<String> motsCible = Arrays.stream(cible.split(" ")).filter(mot -> !mot.isEmpty()).collect(Collectors.toList());
        boolean tousLesMots = motsCible.stream()
                .allMatch(mot -> motsCandidat.stream().anyMatch(motCandidat -> motCandidat.equals(mot)
                        || (!mot.chars().allMatch(Character::isDigit) && motCandidat.startsWith(mot))));
        if (tousLesMots) {
            return SCORE_MOTS;
        }
        if (cible.length() >= LONGUEUR_MIN_INCLUSION && candidat.contains(cible)) {
            return SCORE_INCLUSION;
        }
        return 0;
    }
}
