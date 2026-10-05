package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.RisqueResponse;
import mg.gov.mef.dsi.gpdi.dto.response.SolutionRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.IaRecommandationMapper;
import mg.gov.mef.dsi.gpdi.mapper.RisqueMapper;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.service.RisqueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RisqueServiceImpl implements RisqueService {

    private final ProjetRepository projetRepository;
    private final RisqueRepository risqueRepository;
    private final IaRecommandationRepository iaRecommandationRepository;
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
}
