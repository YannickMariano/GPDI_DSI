package mg.gov.mef.dsi.gpdi.service.impl;

import mg.gov.mef.dsi.gpdi.dto.request.PointInformationRequest;
import mg.gov.mef.dsi.gpdi.dto.response.PointInformationResponse;
import mg.gov.mef.dsi.gpdi.entity.PointInformation;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesPointsInformationRefuseException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.PointInformationMapper;
import mg.gov.mef.dsi.gpdi.repository.PartageClientRepository;
import mg.gov.mef.dsi.gpdi.repository.PointInformationRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointInformationServiceImplTest {

    private static final Long ID_PROJET = 1L;
    private static final Long ID_RESPONSABLE = 10L;
    private static final Long ID_CLIENT = 20L;
    private static final Long ID_AUTRE = 30L;

    @Mock
    private PointInformationRepository pointInformationRepository;
    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private PartageClientRepository partageClientRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourantService;

    private PointInformationServiceImpl service;
    private Projet projet;
    private Utilisateur responsable;
    private Utilisateur client;
    private Utilisateur autre;

    @BeforeEach
    void setUp() {
        service = new PointInformationServiceImpl(pointInformationRepository, projetRepository, partageClientRepository,
                Mappers.getMapper(PointInformationMapper.class), utilisateurCourantService);

        responsable = new Utilisateur();
        responsable.setId(ID_RESPONSABLE);
        responsable.setNom("Rabe");
        responsable.setPrenom("Lala");

        client = new Utilisateur();
        client.setId(ID_CLIENT);

        autre = new Utilisateur();
        autre.setId(ID_AUTRE);

        projet = new Projet();
        projet.setId(ID_PROJET);
        projet.setResponsable(responsable);

        lenient().when(projetRepository.findById(ID_PROJET)).thenReturn(Optional.of(projet));
    }

    private PointInformationRequest requete(String titre, String corps) {
        PointInformationRequest request = new PointInformationRequest();
        request.setTitre(titre);
        request.setCorps(corps);
        return request;
    }

    @Test
    void creer_responsableDuProjet_enregistreLePointEtLeRattacheAuProjetEtALAuteur() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(responsable);
        when(pointInformationRepository.save(any(PointInformation.class))).thenAnswer(invocation -> {
            PointInformation point = invocation.getArgument(0);
            point.setId(99L);
            return point;
        });

        PointInformationResponse reponse = service.creer(ID_PROJET, requete("Livraison", "Le module est en ligne."));

        ArgumentCaptor<PointInformation> captor = ArgumentCaptor.forClass(PointInformation.class);
        verify(pointInformationRepository).save(captor.capture());
        assertThat(captor.getValue().getProjet()).isSameAs(projet);
        assertThat(captor.getValue().getAuteur()).isSameAs(responsable);
        assertThat(captor.getValue().getTitre()).isEqualTo("Livraison");
        assertThat(captor.getValue().getCorps()).isEqualTo("Le module est en ligne.");
        assertThat(reponse.getId()).isEqualTo(99L);
        assertThat(reponse.getIdAuteur()).isEqualTo(ID_RESPONSABLE);
        assertThat(reponse.getNomAuteur()).isEqualTo("Lala Rabe");
    }

    @Test
    void creer_utilisateurNonResponsableDuProjet_leveAccesPointsInformationRefuseException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(autre);

        assertThatThrownBy(() -> service.creer(ID_PROJET, requete("Titre", "Corps")))
                .isInstanceOf(AccesPointsInformationRefuseException.class);

        verify(pointInformationRepository, never()).save(any(PointInformation.class));
    }

    @Test
    void creer_projetInconnu_leveProjetIntrouvableException() {
        when(projetRepository.findById(99L)).thenReturn(Optional.<Projet>empty());

        assertThatThrownBy(() -> service.creer(99L, requete("Titre", "Corps")))
                .isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void rechercher_responsableDuProjet_retourneLaPage() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(responsable);
        Pageable pageable = PageRequest.of(0, 10);
        Page<PointInformation> page = new PageImpl<>(Collections.singletonList(pointInformation("Titre")));
        when(pointInformationRepository.findByProjetIdOrderByDateCreationDesc(ID_PROJET, pageable)).thenReturn(page);

        Page<PointInformationResponse> reponse = service.rechercher(ID_PROJET, pageable);

        assertThat(reponse.getContent()).hasSize(1);
        assertThat(reponse.getContent().get(0).getTitre()).isEqualTo("Titre");
    }

    @Test
    void rechercher_clientAvecPartageActif_retourneLaPage() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(client);
        when(partageClientRepository.existsByClientIdAndProjetIdAndStatut(ID_CLIENT, ID_PROJET, StatutPartage.ACTIF)).thenReturn(true);
        Pageable pageable = PageRequest.of(0, 10);
        when(pointInformationRepository.findByProjetIdOrderByDateCreationDesc(ID_PROJET, pageable))
                .thenReturn(new PageImpl<>(Collections.singletonList(pointInformation("Titre"))));

        Page<PointInformationResponse> reponse = service.rechercher(ID_PROJET, pageable);

        assertThat(reponse.getContent()).hasSize(1);
    }

    @Test
    void rechercher_utilisateurSansPartageActifNiResponsabilite_leveAccesPointsInformationRefuseException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(autre);
        when(partageClientRepository.existsByClientIdAndProjetIdAndStatut(ID_AUTRE, ID_PROJET, StatutPartage.ACTIF)).thenReturn(false);
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.rechercher(ID_PROJET, pageable))
                .isInstanceOf(AccesPointsInformationRefuseException.class);

        verify(pointInformationRepository, never()).findByProjetIdOrderByDateCreationDesc(any(Long.class), any(Pageable.class));
    }

    private PointInformation pointInformation(String titre) {
        PointInformation point = new PointInformation();
        point.setId(1L);
        point.setProjet(projet);
        point.setTitre(titre);
        point.setCorps("Corps");
        return point;
    }
}
