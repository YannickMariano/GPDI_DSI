package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.SousTacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.SousTacheResponse;
import mg.gov.mef.dsi.gpdi.entity.SousTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.exception.SousTacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.TacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.TacheMapper;
import mg.gov.mef.dsi.gpdi.repository.SousTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.SousTacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SousTacheServiceImpl implements SousTacheService {

    private static final Logger log = LoggerFactory.getLogger(SousTacheServiceImpl.class);

    private final SousTacheRepository sousTacheRepository;
    private final TacheRepository tacheRepository;
    private final TacheMapper tacheMapper;
    private final AccesProjetService accesProjetService;

    @Override
    @Transactional
    public SousTacheResponse creer(Long idTache, SousTacheRequest request) {
        Tache tache = obtenirTache(idTache);
        accesProjetService.verifierMembreDuProjet(tache.getProjet());

        SousTache sousTache = new SousTache();
        sousTache.setTitre(request.getTitre());
        sousTache.setFait(false);
        sousTache.setTache(tache);

        SousTacheResponse reponse = tacheMapper.toSousTacheResponse(sousTacheRepository.save(sousTache));
        log.info("Sous-tache {} creee pour la tache {}", reponse.getId(), idTache);
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SousTacheResponse> rechercher(Long idTache) {
        Tache tache = obtenirTache(idTache);
        accesProjetService.verifierAccesLecture(tache.getProjet());
        return sousTacheRepository.findByTacheId(idTache).stream()
                .map(tacheMapper::toSousTacheResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void supprimer(Long idTache, Long id) {
        SousTache sousTache = obtenirSousTacheDeLaTache(idTache, id);
        accesProjetService.verifierMembreDuProjet(sousTache.getTache().getProjet());
        sousTacheRepository.delete(sousTache);
        log.info("Sous-tache {} supprimee", id);
    }

    @Override
    @Transactional
    public SousTacheResponse basculer(Long idTache, Long id) {
        SousTache sousTache = obtenirSousTacheDeLaTache(idTache, id);
        accesProjetService.verifierMembreDuProjet(sousTache.getTache().getProjet());
        sousTache.setFait(!Boolean.TRUE.equals(sousTache.getFait()));
        SousTacheResponse reponse = tacheMapper.toSousTacheResponse(sousTacheRepository.save(sousTache));
        log.info("Sous-tache {} basculee a fait={}", id, reponse.getFait());
        return reponse;
    }

    private SousTache obtenirSousTacheDeLaTache(Long idTache, Long id) {
        SousTache sousTache = sousTacheRepository.findById(id)
                .orElseThrow(() -> new SousTacheIntrouvableException(id));
        if (sousTache.getTache() == null || !sousTache.getTache().getId().equals(idTache)) {
            throw new SousTacheIntrouvableException(id);
        }
        return sousTache;
    }

    private Tache obtenirTache(Long idTache) {
        return tacheRepository.findById(idTache)
                .orElseThrow(() -> new TacheIntrouvableException(idTache));
    }
}
