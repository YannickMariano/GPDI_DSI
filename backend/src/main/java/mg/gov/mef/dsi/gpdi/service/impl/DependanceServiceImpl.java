package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.DependanceRequest;
import mg.gov.mef.dsi.gpdi.dto.response.TachePrecedenceResponse;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TachePrecedence;
import mg.gov.mef.dsi.gpdi.entity.TachePrecedenceId;
import mg.gov.mef.dsi.gpdi.exception.TacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.TachePrecedenceDejaExistanteException;
import mg.gov.mef.dsi.gpdi.exception.TachePrecedenceIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.TachePrecedenceInvalideException;
import mg.gov.mef.dsi.gpdi.mapper.TacheMapper;
import mg.gov.mef.dsi.gpdi.repository.TachePrecedenceRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.DependanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DependanceServiceImpl implements DependanceService {

    private static final Logger log = LoggerFactory.getLogger(DependanceServiceImpl.class);

    private final TachePrecedenceRepository tachePrecedenceRepository;
    private final TacheRepository tacheRepository;
    private final TacheMapper tacheMapper;
    private final AccesProjetService accesProjetService;

    @Override
    @Transactional
    public TachePrecedenceResponse creer(Long idTache, DependanceRequest request) {
        Long idTacheCible = request.getIdTacheCible();
        if (idTache.equals(idTacheCible)) {
            throw new TachePrecedenceInvalideException("Une tache ne peut pas dependre d'elle-meme.");
        }

        Tache source = tacheRepository.findById(idTache)
                .orElseThrow(() -> new TacheIntrouvableException(idTache));
        accesProjetService.verifierMembreDuProjet(source.getProjet());
        Tache cible = tacheRepository.findById(idTacheCible)
                .orElseThrow(() -> new TacheIntrouvableException(idTacheCible));

        if (tachePrecedenceRepository.existsByTacheSourceIdAndTacheCibleId(idTache, idTacheCible)
                || tachePrecedenceRepository.existsByTacheSourceIdAndTacheCibleId(idTacheCible, idTache)) {
            throw new TachePrecedenceDejaExistanteException(idTache, idTacheCible);
        }

        TachePrecedence precedence = new TachePrecedence();
        precedence.setId(new TachePrecedenceId(idTache, idTacheCible));
        precedence.setTacheSource(source);
        precedence.setTacheCible(cible);
        precedence.setTypeLien(request.getTypeLien());

        TachePrecedenceResponse reponse = tacheMapper.toPrecedenceResponse(tachePrecedenceRepository.save(precedence));
        log.info("Dependance ajoutee entre la tache {} et la tache {}", idTache, idTacheCible);
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TachePrecedenceResponse> rechercher(Long idTache) {
        Tache tache = tacheRepository.findById(idTache)
                .orElseThrow(() -> new TacheIntrouvableException(idTache));
        accesProjetService.verifierAccesLecture(tache.getProjet());
        return tachePrecedenceRepository.findByTacheSourceIdOrTacheCibleId(idTache, idTache).stream()
                .map(tacheMapper::toPrecedenceResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void supprimer(Long idTache, Long idAutreTache) {
        Tache tache = tacheRepository.findById(idTache)
                .orElseThrow(() -> new TacheIntrouvableException(idTache));
        accesProjetService.verifierMembreDuProjet(tache.getProjet());

        TachePrecedenceId idDirect = new TachePrecedenceId(idTache, idAutreTache);
        TachePrecedenceId idInverse = new TachePrecedenceId(idAutreTache, idTache);

        if (tachePrecedenceRepository.existsById(idDirect)) {
            tachePrecedenceRepository.deleteById(idDirect);
        } else if (tachePrecedenceRepository.existsById(idInverse)) {
            tachePrecedenceRepository.deleteById(idInverse);
        } else {
            throw new TachePrecedenceIntrouvableException(idTache, idAutreTache);
        }
        log.info("Dependance supprimee entre la tache {} et la tache {}", idTache, idAutreTache);
    }
}
