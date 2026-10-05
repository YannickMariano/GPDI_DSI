package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.BurndownPointResponse;
import mg.gov.mef.dsi.gpdi.entity.BurndownSnapshot;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.exception.SprintIntrouvableException;
import mg.gov.mef.dsi.gpdi.repository.BurndownSnapshotRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.service.BurndownService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BurndownServiceImpl implements BurndownService {

    private final SprintRepository sprintRepository;
    private final TacheRepository tacheRepository;
    private final BurndownSnapshotRepository burndownSnapshotRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BurndownPointResponse> obtenir(Long idProjet, Long idSprint) {
        Sprint sprint = obtenirSprintDuProjet(idProjet, idSprint);
        LocalDate debut = sprint.getDateDebut();
        LocalDate fin = sprint.getDateFin();
        if (debut == null || fin == null || fin.isBefore(debut)) {
            return Collections.emptyList();
        }

        Map<LocalDate, Double> restantsParJour = burndownSnapshotRepository.findBySprintIdOrderByDateAsc(idSprint).stream()
                .collect(Collectors.toMap(BurndownSnapshot::getDate, BurndownSnapshot::getHeuresRestantes));

        double totalEstimeHeures = tacheRepository.findBySprintId(idSprint).stream()
                .mapToDouble(t -> t.getTempsEstime() != null ? t.getTempsEstime() : 0.0)
                .sum();

        long totalJours = ChronoUnit.DAYS.between(debut, fin) + 1;

        List<BurndownPointResponse> points = new ArrayList<>();
        for (long i = 0; i < totalJours; i++) {
            LocalDate jour = debut.plusDays(i);
            double ideal = totalJours <= 1
                    ? 0
                    : totalEstimeHeures * (totalJours - 1 - i) / (totalJours - 1);
            points.add(new BurndownPointResponse(jour, restantsParJour.get(jour), Math.max(0, ideal)));
        }
        return points;
    }

    private Sprint obtenirSprintDuProjet(Long idProjet, Long idSprint) {
        Sprint sprint = sprintRepository.findById(idSprint)
                .orElseThrow(() -> new SprintIntrouvableException(idSprint));
        if (sprint.getProjet() == null || !sprint.getProjet().getId().equals(idProjet)) {
            throw new SprintIntrouvableException(idSprint);
        }
        return sprint;
    }
}
