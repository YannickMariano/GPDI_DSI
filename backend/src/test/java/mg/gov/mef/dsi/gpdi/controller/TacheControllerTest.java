package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
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
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class TacheControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_TACHE";

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
    private TacheRepository tacheRepository;

    @Autowired
    private mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository affectationTacheRepository;

    @Autowired
    private mg.gov.mef.dsi.gpdi.repository.TachePrecedenceRepository tachePrecedenceRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private EquipeRepository equipeRepository;

    @Autowired
    private MembreEquipeRepository membreEquipeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private HabilitationRepository habilitationRepository;

    private Long idProjet;
    private Long idMembreEquipe;
    private Long idUtilisateurHorsEquipe;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        affectationTacheRepository.deleteAll();
        tachePrecedenceRepository.deleteAll();
        tacheRepository.deleteAll();
        membreEquipeRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();
        habilitationRepository.deleteAll();

        for (String fonction : new String[]{"Création des tâches", "Affectation des tâches",
                "Priorisation des tâches", "Changement de statut", "Enregistrement du temps passé"}) {
            Habilitation habilitation = new Habilitation();
            habilitation.setModule("Module 2");
            habilitation.setFonction(fonction);
            habilitation.setRoleNom("chef_projet");
            habilitation.setNiveau(NiveauAcces.ECRITURE);
            habilitationRepository.save(habilitation);
        }

        Role roleChefProjet = new Role();
        roleChefProjet.setNom("chef_projet");
        roleChefProjet.setDescription("Chef de projet");
        Role roleChefProjetEnregistre = roleRepository.save(roleChefProjet);

        Role roleDeveloppeur = new Role();
        roleDeveloppeur.setNom("membre_equipe");
        roleDeveloppeur.setDescription("Developpeur");
        Role roleDeveloppeurEnregistre = roleRepository.save(roleDeveloppeur);

        Utilisateur responsable = new Utilisateur();
        responsable.setMatricule("70001");
        responsable.setNom("Nom");
        responsable.setPrenom("Prenom");
        responsable.setEmail("resp70001@test.mg");
        responsable.setMotDePasse("HASH");
        responsable.setRole(roleChefProjetEnregistre);
        Utilisateur responsableEnregistre = utilisateurRepository.save(responsable);

        Utilisateur membre = new Utilisateur();
        membre.setMatricule("70002");
        membre.setNom("Membre");
        membre.setPrenom("Equipe");
        membre.setEmail("membre70002@test.mg");
        membre.setMotDePasse("HASH");
        membre.setRole(roleDeveloppeurEnregistre);
        Utilisateur membreEnregistre = utilisateurRepository.save(membre);

        Utilisateur horsEquipe = new Utilisateur();
        horsEquipe.setMatricule("70003");
        horsEquipe.setNom("Hors");
        horsEquipe.setPrenom("Equipe");
        horsEquipe.setEmail("hors70003@test.mg");
        horsEquipe.setMotDePasse("HASH");
        horsEquipe.setRole(roleDeveloppeurEnregistre);
        idUtilisateurHorsEquipe = utilisateurRepository.save(horsEquipe).getId();

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Tache");
        equipe.setChefEquipe(responsableEnregistre);
        Equipe equipeEnregistree = equipeRepository.save(equipe);

        MembreEquipe membreEquipe = new MembreEquipe();
        membreEquipe.setEquipe(equipeEnregistree);
        membreEquipe.setUtilisateur(membreEnregistre);
        idMembreEquipe = membreEquipeRepository.save(membreEquipe).getUtilisateur().getId();

        Projet projet = new Projet();
        projet.setTitre("Projet Tache");
        projet.setDateDebut(LocalDate.now());
        projet.setDateFin(LocalDate.now().plusMonths(3));
        projet.setPriorite(Priorite.NORMALE);
        projet.setResponsable(responsableEnregistre);
        projet.setEquipe(equipeEnregistree);
        idProjet = projetRepository.save(projet).getId();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor utilisateurConnecte() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", "70001"))
                .authorities(new SimpleGrantedAuthority(ROLE_GESTIONNAIRE));
    }

    private TacheRequest nouvelleRequete(String titre, Long idAssigne) {
        TacheRequest requete = new TacheRequest();
        requete.setTitre(titre);
        requete.setDescription("Description " + titre);
        requete.setPriorite(Priorite.HAUTE);
        requete.setDateEcheance(LocalDate.now().plusWeeks(2));
        requete.setIdsUtilisateursAssignes(idAssigne == null ? Collections.emptyList() : Collections.singletonList(idAssigne));
        return requete;
    }

    @Test
    void creer_membreEligible_retourne201AvecCodeGenere() throws Exception {
        mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache 1", idMembreEquipe))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("T-\\d+")))
                .andExpect(jsonPath("$.statut").value("A_FAIRE"))
                .andExpect(jsonPath("$.assignations", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void creer_utilisateurHorsEquipe_retourne409() throws Exception {
        mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache 2", idUtilisateurHorsEquipe))))
                .andExpect(status().isConflict());
    }

    @Test
    void obtenir_tacheInexistante_retourne404() throws Exception {
        mockMvc.perform(get("/api/projets/{idProjet}/taches/{id}", idProjet, 999999L)
                .with(utilisateurConnecte()))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenir_tacheExistante_retourneLeDetailAvecListesVides() throws Exception {
        String reponse = mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache 3", null))))
                .andReturn().getResponse().getContentAsString();
        Long idTache = objectMapper.readTree(reponse).get("id").asLong();

        mockMvc.perform(get("/api/projets/{idProjet}/taches/{id}", idProjet, idTache)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sousTaches", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.dependances", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.commentaires", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void creer_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache 4", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creer_sansDateEcheance_retourne201() throws Exception {
        TacheRequest requete = nouvelleRequete("Tache sans echeance", null);
        requete.setDateEcheance(null);

        mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dateEcheance").doesNotExist());
    }

    @Test
    void modifier_avecStatutFourni_metAJourLeStatut() throws Exception {
        String reponse = mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache 5", null))))
                .andReturn().getResponse().getContentAsString();
        Long idTache = objectMapper.readTree(reponse).get("id").asLong();

        TacheRequest modification = nouvelleRequete("Tache 5", null);
        modification.setStatut(mg.gov.mef.dsi.gpdi.entity.StatutTache.EN_COURS);

        mockMvc.perform(put("/api/projets/{idProjet}/taches/{id}", idProjet, idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void modifier_sansStatutFourni_conserveLeStatutCourant() throws Exception {
        String reponse = mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache 6", null))))
                .andReturn().getResponse().getContentAsString();
        Long idTache = objectMapper.readTree(reponse).get("id").asLong();

        mockMvc.perform(put("/api/projets/{idProjet}/taches/{id}", idProjet, idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache 6 modifiee", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("A_FAIRE"));
    }

    @Test
    void modifier_passageEnCoursPuisTerminee_demarreEtArreteLeChronometre() throws Exception {
        String reponse = mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache chrono", null))))
                .andReturn().getResponse().getContentAsString();
        Long idTache = objectMapper.readTree(reponse).get("id").asLong();

        TacheRequest demarrage = nouvelleRequete("Tache chrono", null);
        demarrage.setStatut(mg.gov.mef.dsi.gpdi.entity.StatutTache.EN_COURS);

        String reponseDemarree = mockMvc.perform(put("/api/projets/{idProjet}/taches/{id}", idProjet, idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(demarrage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demarreeLe").exists())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertNotNull(objectMapper.readTree(reponseDemarree).get("demarreeLe").asText());

        TacheRequest fin = nouvelleRequete("Tache chrono", null);
        fin.setStatut(mg.gov.mef.dsi.gpdi.entity.StatutTache.TERMINEE);

        mockMvc.perform(put("/api/projets/{idProjet}/taches/{id}", idProjet, idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demarreeLe").doesNotExist())
                .andExpect(jsonPath("$.tempsPasse").isNumber());
    }

    @Test
    void modifier_tempsPasseFourni_metAJourLeTempsPasse() throws Exception {
        String reponse = mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache saisie manuelle", null))))
                .andReturn().getResponse().getContentAsString();
        Long idTache = objectMapper.readTree(reponse).get("id").asLong();

        TacheRequest modification = nouvelleRequete("Tache saisie manuelle", null);
        modification.setTempsPasse(4.5);

        mockMvc.perform(put("/api/projets/{idProjet}/taches/{id}", idProjet, idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempsPasse").value(4.5));
    }

    @Test
    void modifier_demarrageBloqueParDependanceFinDebut_retourne400PuisAutoriseUneFoisSourceTerminee() throws Exception {
        String reponseSource = mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache source", null))))
                .andReturn().getResponse().getContentAsString();
        Long idSource = objectMapper.readTree(reponseSource).get("id").asLong();

        String reponseCible = mockMvc.perform(post("/api/projets/{idProjet}/taches", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Tache cible", null))))
                .andReturn().getResponse().getContentAsString();
        Long idCible = objectMapper.readTree(reponseCible).get("id").asLong();

        mockMvc.perform(post("/api/taches/{idTache}/dependances", idSource)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idTacheCible\":" + idCible + ",\"typeLien\":\"FIN_DEBUT\"}"))
                .andExpect(status().isCreated());

        TacheRequest demarrageCible = nouvelleRequete("Tache cible", null);
        demarrageCible.setStatut(mg.gov.mef.dsi.gpdi.entity.StatutTache.EN_COURS);

        mockMvc.perform(put("/api/projets/{idProjet}/taches/{id}", idProjet, idCible)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(demarrageCible)))
                .andExpect(status().isBadRequest());

        TacheRequest termineSource = nouvelleRequete("Tache source", null);
        termineSource.setStatut(mg.gov.mef.dsi.gpdi.entity.StatutTache.TERMINEE);
        mockMvc.perform(put("/api/projets/{idProjet}/taches/{id}", idProjet, idSource)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(termineSource)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/projets/{idProjet}/taches/{id}", idProjet, idCible)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(demarrageCible)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }
}
