package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.client.ModeleIaClient;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.MessageAssistantResponse;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.PorteeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesAssistantIaGlobalRefuseException;
import mg.gov.mef.dsi.gpdi.exception.AccesAssistantIaRefuseException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.AssistantIaService;
import mg.gov.mef.dsi.gpdi.service.ContexteProjetService;
import mg.gov.mef.dsi.gpdi.util.AssistantIaConsignes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Comme pour les risques, l'appel au modele se fait hors transaction ; l'echange (question + reponse)
 * est ensuite enregistre en une seule transaction, ou pas du tout.
 */
@Service
@RequiredArgsConstructor
public class AssistantIaServiceImpl implements AssistantIaService {

    private static final Logger log = LoggerFactory.getLogger(AssistantIaServiceImpl.class);
    private static final int MAX_LONGUEUR_REPONSE = 4000;
    private static final String ROLE_CHEF_PROJET = "chef_projet";

    private final ProjetRepository projetRepository;
    private final IaAnalyseRepository iaAnalyseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ContexteProjetService contexteProjetService;
    private final ModeleIaClient ollamaClient;
    private final UtilisateurCourantService utilisateurCourantService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<MessageAssistantResponse> historique(Long idProjet) {
        Projet projet = obtenirProjetAccessible(idProjet);
        return iaAnalyseRepository.findByProjetIdAndTypeAnalyseOrderByDateCreationAsc(projet.getId(), TypeAnalyseIa.ANALYSE_PROJET)
                .stream()
                .map(analyse -> new MessageAssistantResponse(analyse.getId(), analyse.getQuestion(), analyse.getReponse(), null, analyse.getDateCreation()))
                .collect(Collectors.toList());
    }

    @Override
    public MessageAssistantResponse poserQuestion(Long idProjet, String question, Long idDemandeur) {
        obtenirProjetAccessible(idProjet);

        ContexteProjetIa contexte = contexteProjetService.construire(idProjet);
        String reponseBrute = ollamaClient.genererJson(AssistantIaConsignes.consigneSysteme(),
                AssistantIaConsignes.consigneUtilisateur(contexte.getContenu(), question), AssistantIaConsignes.schemaReponse());
        JsonNode noeud = lire(reponseBrute);
        String reponseTexte = texte(noeud.path("reponse"));
        if (reponseTexte == null) {
            throw new ReponseIaInvalideException("L'assistant IA n'a pas fourni de réponse exploitable.");
        }
        String fondement = texte(noeud.path("fondement"));

        return transactionTemplate.execute(
                statut -> enregistrer(idProjet, idDemandeur, question, reponseTexte, fondement, contexte.getContenu()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageAssistantResponse> historiqueGlobal() {
        Utilisateur chefDeProjet = obtenirChefDeProjetCourant();
        return iaAnalyseRepository.findByDemandeurIdAndPorteeOrderByDateCreationAsc(chefDeProjet.getId(), PorteeAnalyseIa.GLOBAL)
                .stream()
                .map(analyse -> new MessageAssistantResponse(analyse.getId(), analyse.getQuestion(), analyse.getReponse(), null, analyse.getDateCreation()))
                .collect(Collectors.toList());
    }

    @Override
    public MessageAssistantResponse poserQuestionGlobale(String question, Long idDemandeur) {
        Utilisateur chefDeProjet = obtenirChefDeProjetCourant();
        List<Long> idsProjets = projetRepository.findByResponsableIdOrderByIdAsc(chefDeProjet.getId()).stream()
                .map(Projet::getId)
                .collect(Collectors.toList());

        JsonNode contexte = contexteProjetService.construirePortefeuille(idsProjets);
        String reponseBrute = ollamaClient.genererJson(AssistantIaConsignes.consigneSystemePortefeuille(),
                AssistantIaConsignes.consigneUtilisateurPortefeuille(contexte, question), AssistantIaConsignes.schemaReponse());
        JsonNode noeud = lire(reponseBrute);
        String reponseTexte = texte(noeud.path("reponse"));
        if (reponseTexte == null) {
            throw new ReponseIaInvalideException("L'assistant IA n'a pas fourni de réponse exploitable.");
        }
        String fondement = texte(noeud.path("fondement"));

        return transactionTemplate.execute(
                statut -> enregistrerGlobal(idDemandeur, question, reponseTexte, fondement, contexte));
    }

    private MessageAssistantResponse enregistrerGlobal(Long idDemandeur, String question, String reponseTexte,
                                                       String fondement, JsonNode contenuContexte) {
        Utilisateur demandeur = utilisateurRepository.findById(idDemandeur).orElse(null);

        IaAnalyse analyse = new IaAnalyse();
        analyse.setDemandeur(demandeur);
        analyse.setTypeAnalyse(TypeAnalyseIa.ANALYSE_PROJET);
        analyse.setPortee(PorteeAnalyseIa.GLOBAL);
        analyse.setQuestion(question);
        analyse.setReponse(tronquer(reponseTexte, MAX_LONGUEUR_REPONSE));
        analyse.setContexte(contenuContexte);
        analyse.setModele(ollamaClient.getModele());
        IaAnalyse enregistree = iaAnalyseRepository.save(analyse);

        log.info("Analyse {} : question globale posee a l'assistant IA par l'utilisateur {}", enregistree.getId(), idDemandeur);
        return new MessageAssistantResponse(enregistree.getId(), enregistree.getQuestion(), enregistree.getReponse(), fondement, enregistree.getDateCreation());
    }

    private Utilisateur obtenirChefDeProjetCourant() {
        Utilisateur utilisateurCourant = utilisateurCourantService.obtenirUtilisateurCourant();
        boolean estChefDeProjet = utilisateurCourant.getRole() != null && ROLE_CHEF_PROJET.equals(utilisateurCourant.getRole().getNom());
        if (!estChefDeProjet) {
            throw new AccesAssistantIaGlobalRefuseException();
        }
        return utilisateurCourant;
    }

    /**
     * Le projet et l'utilisateur sont relus dans cette transaction (l'acces a pu changer pendant l'appel au modele).
     */
    private MessageAssistantResponse enregistrer(Long idProjet, Long idDemandeur, String question, String reponseTexte,
                                                   String fondement, JsonNode contenuContexte) {
        Projet projet = obtenirProjetAccessible(idProjet);
        Utilisateur demandeur = utilisateurRepository.findById(idDemandeur).orElse(null);

        IaAnalyse analyse = new IaAnalyse();
        analyse.setProjet(projet);
        analyse.setDemandeur(demandeur);
        analyse.setTypeAnalyse(TypeAnalyseIa.ANALYSE_PROJET);
        analyse.setPortee(PorteeAnalyseIa.PROJET);
        analyse.setQuestion(question);
        analyse.setReponse(tronquer(reponseTexte, MAX_LONGUEUR_REPONSE));
        analyse.setContexte(contenuContexte);
        analyse.setModele(ollamaClient.getModele());
        IaAnalyse enregistree = iaAnalyseRepository.save(analyse);

        log.info("Analyse {} : question posee a l'assistant IA sur le projet {}", enregistree.getId(), idProjet);
        return new MessageAssistantResponse(enregistree.getId(), enregistree.getQuestion(), enregistree.getReponse(), fondement, enregistree.getDateCreation());
    }

    /** Seul le chef de projet (responsable) du projet a acces a son assistant IA. */
    private Projet obtenirProjetAccessible(Long idProjet) {
        Projet projet = projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));
        Utilisateur utilisateurCourant = utilisateurCourantService.obtenirUtilisateurCourant();
        boolean estResponsable = projet.getResponsable() != null && projet.getResponsable().getId().equals(utilisateurCourant.getId());
        if (!estResponsable) {
            throw new AccesAssistantIaRefuseException(idProjet);
        }
        return projet;
    }

    private JsonNode lire(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new ReponseIaInvalideException("La réponse de l'assistant IA n'est pas un JSON exploitable.", e);
        }
    }

    private String texte(JsonNode noeud) {
        return noeud.isTextual() && StringUtils.hasText(noeud.asText()) ? noeud.asText().trim() : null;
    }

    private String tronquer(String valeur, int longueurMax) {
        return valeur.length() <= longueurMax ? valeur : valeur.substring(0, longueurMax - 1) + "…";
    }
}
