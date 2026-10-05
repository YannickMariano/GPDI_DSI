package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.RoleRequest;
import mg.gov.mef.dsi.gpdi.dto.response.RoleResponse;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.exception.RoleIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.RoleMapper;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    private RoleServiceImpl service;

    @BeforeEach
    void initService() {
        service = new RoleServiceImpl(roleRepository, roleMapper);
    }

    @Test
    void creer_requeteValide_enregistreEtRetourneLaReponse() {
        RoleRequest request = new RoleRequest();
        request.setNom("ADMIN");
        request.setDescription("Administrateur");

        Role roleAEnregistrer = new Role();
        Role roleEnregistre = new Role();
        roleEnregistre.setId(1L);
        RoleResponse reponseAttendue = new RoleResponse(1L, "ADMIN", "Administrateur");

        when(roleMapper.toEntity(request)).thenReturn(roleAEnregistrer);
        when(roleRepository.save(roleAEnregistrer)).thenReturn(roleEnregistre);
        when(roleMapper.toResponse(roleEnregistre)).thenReturn(reponseAttendue);

        RoleResponse resultat = service.creer(request);

        assertThat(resultat).isEqualTo(reponseAttendue);
        verify(roleRepository).save(roleAEnregistrer);
    }

    @Test
    void obtenir_roleExistant_retourneLaReponseMappee() {
        Role role = new Role();
        role.setId(1L);
        RoleResponse reponseAttendue = new RoleResponse(1L, "ADMIN", "Administrateur");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleMapper.toResponse(role)).thenReturn(reponseAttendue);

        RoleResponse resultat = service.obtenir(1L);

        assertThat(resultat).isEqualTo(reponseAttendue);
    }

    @Test
    void obtenir_roleInexistant_leveRoleIntrouvableException() {
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenir(999L))
                .isInstanceOf(RoleIntrouvableException.class);
    }

    @Test
    void modifier_roleExistant_metAJourEtRetourneLaReponse() {
        Role role = new Role();
        role.setId(1L);
        RoleRequest request = new RoleRequest();
        request.setNom("GESTIONNAIRE");
        request.setDescription("Gestionnaire");
        RoleResponse reponseAttendue = new RoleResponse(1L, "GESTIONNAIRE", "Gestionnaire");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.toResponse(role)).thenReturn(reponseAttendue);

        RoleResponse resultat = service.modifier(1L, request);

        assertThat(resultat).isEqualTo(reponseAttendue);
        verify(roleMapper).updateEntity(request, role);
    }

    @Test
    void modifier_roleInexistant_leveRoleIntrouvableException() {
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifier(999L, new RoleRequest()))
                .isInstanceOf(RoleIntrouvableException.class);
    }

    @Test
    void supprimer_roleExistant_appelleDeleteById() {
        when(roleRepository.existsById(1L)).thenReturn(true);

        service.supprimer(1L);

        verify(roleRepository).deleteById(1L);
    }

    @Test
    void supprimer_roleInexistant_leveRoleIntrouvableExceptionSansAppelerDelete() {
        when(roleRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.supprimer(999L))
                .isInstanceOf(RoleIntrouvableException.class);
    }
}
