package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.LivrableRejetRequest;
import mg.gov.mef.dsi.gpdi.dto.request.LivrableRequest;
import mg.gov.mef.dsi.gpdi.dto.response.LivrableResponse;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.Livrable;
import mg.gov.mef.dsi.gpdi.entity.StatutJalon;
import mg.gov.mef.dsi.gpdi.entity.StatutLivrable;
import mg.gov.mef.dsi.gpdi.exception.DateLivraisonInvalideException;
import mg.gov.mef.dsi.gpdi.exception.JalonIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.LivrableIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutJalonInvalideException;
import mg.gov.mef.dsi.gpdi.exception.StatutLivrableInvalideException;
import mg.gov.mef.dsi.gpdi.mapper.LivrableMapper;
import mg.gov.mef.dsi.gpdi.repository.JalonRepository;
import mg.gov.mef.dsi.gpdi.repository.LivrableRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.HabilitationService;
import mg.gov.mef.dsi.gpdi.service.LivrableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LivrableServiceImpl implements LivrableService {

    private static final Logger log = LoggerFactory.getLogger(LivrableServiceImpl.class);

    private static final String FONCTION_JALONS_LIVRABLES = "Gestion des jalons et livrables";

    private final LivrableRepository livrableRepository;
    private final JalonRepository jalonRepository;
    private final LivrableMapper livrableMapper;
    private final AccesProjetService accesProjetService;
    private final HabilitationService habilitationService;

    @Override
    @Transactional
    public LivrableResponse creer(Long idJalon, LivrableRequest request) {
        Jalon jalon = obtenirJalon(idJalon);
        accesProjetService.verifierMembreDuProjet(jalon.getProjet());
        habilitationService.verifierEcriture(FONCTION_JALONS_LIVRABLES);
        if (jalon.getStatut() == StatutJalon.ATTEINT) {
            throw new StatutJalonInvalideException(idJalon, jalon.getStatut(), "utilise pour ajouter un livrable");
        }
        validerDateLivraison(request.getDateLivraison(), jalon);

        Livrable livrable = livrableMapper.toEntity(request);
        livrable.setJalon(jalon);

        LivrableResponse reponse = livrableMapper.toResponse(livrableRepository.save(livrable));
        log.info("Livrable {} cree pour le jalon {}", reponse.getId(), idJalon);
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LivrableResponse> rechercher(Long idJalon, Pageable pageable) {
        Jalon jalon = obtenirJalon(idJalon);
        accesProjetService.verifierAccesLecturePartageable(jalon.getProjet());
        return livrableRepository.findByJalonId(idJalon, pageable).map(livrableMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LivrableResponse obtenir(Long idJalon, Long id) {
        Livrable livrable = obtenirLivrableDuJalon(idJalon, id);
        accesProjetService.verifierAccesLecturePartageable(livrable.getJalon().getProjet());
        return livrableMapper.toResponse(livrable);
    }

    @Override
    @Transactional
    public LivrableResponse modifier(Long idJalon, Long id, LivrableRequest request) {
        Livrable livrable = obtenirLivrableDuJalon(idJalon, id);
        accesProjetService.verifierMembreDuProjet(livrable.getJalon().getProjet());
        habilitationService.verifierEcriture(FONCTION_JALONS_LIVRABLES);
        validerDateLivraison(request.getDateLivraison(), livrable.getJalon());

        livrableMapper.updateEntity(request, livrable);

        LivrableResponse reponse = livrableMapper.toResponse(livrableRepository.save(livrable));
        log.info("Livrable {} modifie", id);
        return reponse;
    }

    @Override
    @Transactional
    public void supprimer(Long idJalon, Long id) {
        Livrable livrable = obtenirLivrableDuJalon(idJalon, id);
        accesProjetService.verifierMembreDuProjet(livrable.getJalon().getProjet());
        habilitationService.verifierEcriture(FONCTION_JALONS_LIVRABLES);
        livrableRepository.delete(livrable);
        log.info("Livrable {} supprime", id);
    }

    @Override
    @Transactional
    public LivrableResponse livrer(Long idJalon, Long id) {
        Livrable livrable = obtenirLivrableDuJalon(idJalon, id);
        accesProjetService.verifierMembreDuProjet(livrable.getJalon().getProjet());
        habilitationService.verifierEcriture(FONCTION_JALONS_LIVRABLES);
        if (livrable.getStatut() != StatutLivrable.ATTENDU) {
            throw new StatutLivrableInvalideException(id, livrable.getStatut(), "marque comme livre");
        }
        livrable.setStatut(StatutLivrable.LIVRE);
        livrable.setDateValidation(LocalDate.now());

        LivrableResponse reponse = livrableMapper.toResponse(livrableRepository.save(livrable));
        log.info("Livrable {} livre", id);
        return reponse;
    }

    @Override
    @Transactional
    public LivrableResponse rejeter(Long idJalon, Long id, LivrableRejetRequest request) {
        Livrable livrable = obtenirLivrableDuJalon(idJalon, id);
        accesProjetService.verifierMembreDuProjet(livrable.getJalon().getProjet());
        habilitationService.verifierEcriture(FONCTION_JALONS_LIVRABLES);
        if (livrable.getStatut() != StatutLivrable.ATTENDU) {
            throw new StatutLivrableInvalideException(id, livrable.getStatut(), "rejete");
        }
        livrable.setStatut(StatutLivrable.REJETE);
        livrable.setDateRejet(LocalDate.now());
        livrable.setMotifRejet(request.getMotifRejet());

        LivrableResponse reponse = livrableMapper.toResponse(livrableRepository.save(livrable));
        log.info("Livrable {} rejete", id);
        return reponse;
    }

    private Jalon obtenirJalon(Long idJalon) {
        return jalonRepository.findById(idJalon)
                .orElseThrow(() -> new JalonIntrouvableException(idJalon));
    }

    private Livrable obtenirLivrableDuJalon(Long idJalon, Long id) {
        Livrable livrable = livrableRepository.findById(id)
                .orElseThrow(() -> new LivrableIntrouvableException(id));
        if (livrable.getJalon() == null || !livrable.getJalon().getId().equals(idJalon)) {
            throw new LivrableIntrouvableException(id);
        }
        return livrable;
    }

    private void validerDateLivraison(LocalDate dateLivraison, Jalon jalon) {
        if (dateLivraison != null && jalon.getDateEcheance() != null
                && dateLivraison.isAfter(jalon.getDateEcheance())) {
            throw new DateLivraisonInvalideException(dateLivraison, jalon.getDateEcheance());
        }
    }
}
