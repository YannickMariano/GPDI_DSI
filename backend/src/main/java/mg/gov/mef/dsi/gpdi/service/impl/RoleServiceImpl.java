package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.RoleRequest;
import mg.gov.mef.dsi.gpdi.dto.response.RoleResponse;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.exception.RoleIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.RoleMapper;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponse creer(RoleRequest request) {
        Role role = roleMapper.toEntity(request);
        RoleResponse reponse = roleMapper.toResponse(roleRepository.save(role));
        log.info("Role {} cree", reponse.getId());
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> rechercher(Pageable pageable) {
        return roleRepository.findAll(pageable).map(roleMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse obtenir(Long id) {
        return roleRepository.findById(id)
                .map(roleMapper::toResponse)
                .orElseThrow(() -> new RoleIntrouvableException(id));
    }

    @Override
    @Transactional
    public RoleResponse modifier(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleIntrouvableException(id));
        roleMapper.updateEntity(request, role);
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void supprimer(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new RoleIntrouvableException(id);
        }
        roleRepository.deleteById(id);
        log.info("Role {} supprime", id);
    }
}