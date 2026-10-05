package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.CommentaireRequest;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.CommentaireRepository;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class CommentaireControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_TACHE";
    private static final String MATRICULE_AUTEUR = "90001";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void proprietesBaseDeDonnees(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CommentaireRepository commentaireRepository;
    @Autowired
    private mg.gov.mef.dsi.gpdi.repository.PieceJointeCommentaireRepository pieceJointeCommentaireRepository;
    @Autowired
    private TacheRepository tacheRepository;
    @Autowired
    private ProjetRepository projetRepository;
    @Autowired
    private EquipeRepository equipeRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private RoleRepository roleRepository;

    private Long idTache;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        pieceJointeCommentaireRepository.deleteAll();
        commentaireRepository.deleteAll();
        tacheRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("CHEF_DE_PROJET");
        role.setDescription("Chef de projet");
        Role roleEnregistre = roleRepository.save(role);

        Utilisateur auteur = new Utilisateur();
        auteur.setMatricule(MATRICULE_AUTEUR);
        auteur.setNom("Nom");
        auteur.setPrenom("Prenom");
        auteur.setEmail("auteur90001@test.mg");
        auteur.setMotDePasse("HASH");
        auteur.setRole(roleEnregistre);
        Utilisateur auteurEnregistre = utilisateurRepository.save(auteur);

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Commentaire");
        equipe.setChefEquipe(auteurEnregistre);
        Equipe equipeEnregistree = equipeRepository.save(equipe);

        Projet projet = new Projet();
        projet.setTitre("Projet Commentaire");
        projet.setDateDebut(LocalDate.now());
        projet.setDateFin(LocalDate.now().plusMonths(3));
        projet.setPriorite(Priorite.NORMALE);
        projet.setResponsable(auteurEnregistre);
        projet.setEquipe(equipeEnregistree);
        Projet projetEnregistre = projetRepository.save(projet);

        Tache tache = new Tache();
        tache.setTitre("Tache Commentee");
        tache.setPriorite(Priorite.NORMALE);
        tache.setDateFinPrevue(LocalDate.now().plusWeeks(2));
        tache.setProjet(projetEnregistre);
        idTache = tacheRepository.save(tache).getId();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor utilisateurConnecte() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", MATRICULE_AUTEUR))
                .authorities(new SimpleGrantedAuthority(ROLE_GESTIONNAIRE));
    }

    @Test
    void creer_auteurExistant_retourne201AvecNomAuteur() throws Exception {
        CommentaireRequest requete = new CommentaireRequest();
        requete.setContenu("Premier commentaire");

        mockMvc.perform(post("/api/taches/{idTache}/commentaires", idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenu").value("Premier commentaire"))
                .andExpect(jsonPath("$.nomAuteur").value("Prenom Nom"));
    }

    @Test
    void rechercher_apresDeuxCommentaires_lesRetourneParOrdreChronologique() throws Exception {
        CommentaireRequest premier = new CommentaireRequest();
        premier.setContenu("Premier");
        mockMvc.perform(post("/api/taches/{idTache}/commentaires", idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(premier)));

        CommentaireRequest second = new CommentaireRequest();
        second.setContenu("Second");
        mockMvc.perform(post("/api/taches/{idTache}/commentaires", idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(second)));

        mockMvc.perform(get("/api/taches/{idTache}/commentaires", idTache)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].contenu").value("Premier"))
                .andExpect(jsonPath("$[1].contenu").value("Second"));
    }

    @Test
    void creer_sansAuthentification_retourne401() throws Exception {
        CommentaireRequest requete = new CommentaireRequest();
        requete.setContenu("Commentaire refuse");

        mockMvc.perform(post("/api/taches/{idTache}/commentaires", idTache)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creerAvecPieceJointe_fichierValide_retourne201AvecMetadonnees() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "cahier-des-charges.pdf", "application/pdf", "contenu du fichier".getBytes());

        mockMvc.perform(multipart("/api/taches/{idTache}/commentaires/avec-piece-jointe", idTache)
                .file(fichier)
                .param("contenu", "Voici le document")
                .with(utilisateurConnecte()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenu").value("Voici le document"))
                .andExpect(jsonPath("$.pieceJointe.nomFichier").value("cahier-des-charges.pdf"))
                .andExpect(jsonPath("$.pieceJointe.typeContenu").value("application/pdf"))
                .andExpect(jsonPath("$.pieceJointe.taille").value("contenu du fichier".getBytes().length));
    }

    @Test
    void creerAvecPieceJointe_sansContenuTexte_retourne201AvecContenuVide() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/taches/{idTache}/commentaires/avec-piece-jointe", idTache)
                .file(fichier)
                .with(utilisateurConnecte()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenu").value(""))
                .andExpect(jsonPath("$.pieceJointe.nomFichier").value("photo.png"));
    }

    @Test
    void creerAvecPieceJointe_fichierVide_retourne400() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "vide.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/taches/{idTache}/commentaires/avec-piece-jointe", idTache)
                .file(fichier)
                .with(utilisateurConnecte()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void telechargerPieceJointe_existante_retourneLeContenuBinaire() throws Exception {
        byte[] contenu = "contenu du fichier".getBytes();
        MockMultipartFile fichier = new MockMultipartFile("fichier", "cahier-des-charges.pdf", "application/pdf", contenu);

        String reponse = mockMvc.perform(multipart("/api/taches/{idTache}/commentaires/avec-piece-jointe", idTache)
                .file(fichier)
                .with(utilisateurConnecte()))
                .andReturn().getResponse().getContentAsString();
        Long idCommentaire = objectMapper.readTree(reponse).get("id").asLong();

        mockMvc.perform(get("/api/taches/{idTache}/commentaires/{idCommentaire}/piece-jointe", idTache, idCommentaire)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("cahier-des-charges.pdf")))
                .andExpect(content -> assertThatContentMatches(content, contenu));
    }

    @Test
    void telechargerPieceJointe_commentaireSansPieceJointe_retourne404() throws Exception {
        CommentaireRequest requete = new CommentaireRequest();
        requete.setContenu("Sans piece jointe");
        String reponse = mockMvc.perform(post("/api/taches/{idTache}/commentaires", idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andReturn().getResponse().getContentAsString();
        Long idCommentaire = objectMapper.readTree(reponse).get("id").asLong();

        mockMvc.perform(get("/api/taches/{idTache}/commentaires/{idCommentaire}/piece-jointe", idTache, idCommentaire)
                .with(utilisateurConnecte()))
                .andExpect(status().isNotFound());
    }

    private static void assertThatContentMatches(org.springframework.test.web.servlet.MvcResult result, byte[] attendu) {
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(attendu);
    }
}
