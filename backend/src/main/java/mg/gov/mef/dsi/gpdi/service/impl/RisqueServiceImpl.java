package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.RisqueResponse;
import mg.gov.mef.dsi.gpdi.dto.response.SolutionRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.IaRecommandationMapper;
import mg.gov.mef.dsi.gpdi.mapper.RisqueMapper;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.service.RisqueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RisqueServiceImpl implements RisqueService {

    /** Une tache "terminee ou reportee" n'evoluera plus : c'est ce qui vaut pour "la mitigation est finie". */
    private static final Set<StatutTache> STATUTS_TACHE_FINIE = EnumSet.of(StatutTache.TERMINEE, StatutTache.REPORTEE);

    private final ProjetRepository projetRepository;
    private final RisqueRepository risqueRepository;
    private final IaRecommandationRepository iaRecommandationRepository;
    private final TacheRepository tacheRepository;
    private final RisqueMapper risqueMapper;
    private final IaRecommandationMapper iaRecommandationMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RisqueResponse> rechercher(Long idProjet) {
        if (!projetRepository.existsById(idProjet)) {
            throw new ProjetIntrouvableException(idProjet);
        }
        List<Risque> risques = risqueRepository.findByProjetIdOrderByIdAsc(idProjet);
        if (risques.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> idsRisques = risques.stream().map(Risque::getId).collect(Collectors.toList());
        // Historique complet (proposees, appliquees, rejetees) : le frontend affiche les solutions deja
        // traitees en lecture seule, sans limiter aux seules solutions encore en attente.
        Map<Long, List<SolutionRisqueResponse>> solutionsParRisque = new HashMap<>();
        for (IaRecommandation solution : iaRecommandationRepository.findByRisqueIdInOrderByIdAsc(idsRisques)) {
            solutionsParRisque.computeIfAbsent(solution.getRisque().getId(), cle -> new ArrayList<>())
                    .add(iaRecommandationMapper.toSolutionResponse(solution));
        }

        List<RisqueResponse> reponses = new ArrayList<>();
        for (int rang = 0; rang < risques.size(); rang++) {
            Risque risque = risques.get(rang);
            RisqueResponse reponse = risqueMapper.toResponse(risque);
            reponse.setCode("R" + (rang + 1));
            reponse.setSolutions(solutionsParRisque.getOrDefault(risque.getId(), Collections.<SolutionRisqueResponse>emptyList()));
            reponses.add(reponse);
        }
        return reponses;
    }

    @Override
    @Transactional
    public void cloreSiTachesTerminees(Long idProjet, Long idTache) {
        for (Risque risque : risqueRepository.findByProjetIdAndStatut(idProjet, StatutRisque.SOUS_CONTROLE)) {
            List<IaRecommandation> appliquees = iaRecommandationRepository.findByRisqueIdAndStatut(risque.getId(), StatutRecommandation.APPLIQUEE);
            if (appliquees.isEmpty()) {
                continue;
            }

            boolean concerneParCetteTache = false;
            boolean toutesLesTachesSontFinies = true;
            for (IaRecommandation solution : appliquees) {
                Long idTacheSolution = idTacheResultat(solution);
                if (idTacheSolution == null) {
                    toutesLesTachesSontFinies = false;
                    continue;
                }
                if (idTacheSolution.equals(idTache)) {
                    concerneParCetteTache = true;
                }
                Tache tache = tacheRepository.findById(idTacheSolution).orElse(null);
                if (tache == null || !STATUTS_TACHE_FINIE.contains(tache.getStatut())) {
                    toutesLesTachesSontFinies = false;
                }
            }

            if (concerneParCetteTache && toutesLesTachesSontFinies) {
                risque.setStatut(StatutRisque.CLOS);
                risqueRepository.save(risque);
            }
        }
    }

    /** Identifiant de la tache creee ou reassignee par une solution appliquee (cf. TraitementRisqueServiceImpl.decrireResultat). */
    private Long idTacheResultat(IaRecommandation solution) {
        JsonNode resultat = solution.getResultatApplication();
        if (resultat == null) {
            return null;
        }
        JsonNode idTache = resultat.path("idTache");
        return idTache.isIntegralNumber() ? idTache.longValue() : null;
    }
}
