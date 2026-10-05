import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProjectsService } from '@core/services/projects.service';
import { PeopleService } from '@core/services/people.service';
import { MilestonesService } from '@core/services/milestones.service';
import { SprintsService } from '@core/services/sprints.service';
import { TasksService } from '@core/services/tasks.service';
import { KpiCard } from '@shared/components/kpi-card/kpi-card';
import { ProgressBar } from '@shared/components/progress-bar/progress-bar';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { SANTE_CONFIG } from '@shared/utils/sante-config';

@Component({
  selector: 'app-dashboard-dsi-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, KpiCard, ProgressBar, EmptyState],
  templateUrl: './dashboard-dsi-page.html',
  styleUrl: './dashboard-dsi-page.css',
})
export class DashboardDsiPage {
  private readonly projectsService = inject(ProjectsService);
  private readonly peopleService = inject(PeopleService);
  private readonly milestonesService = inject(MilestonesService);
  private readonly sprintsService = inject(SprintsService);
  private readonly tasksService = inject(TasksService);

  protected readonly santeConfig = SANTE_CONFIG;

  /** Heures consommées par sprint, agrégées sur le portefeuille : S-1 = sprint le plus récent (courant ou clôturé) de chaque projet, S-2 le précédent, etc. */
  protected readonly velocity = computed(() => {
    const sprints = this.sprintsService.sprints();
    const taches = this.tasksService.tasks();
    const heuresParSprint = new Map<string, number>();
    for (const t of taches) if (t.sprintId) heuresParSprint.set(t.sprintId, (heuresParSprint.get(t.sprintId) ?? 0) + t.tempsPasseHeures);

    const heuresParRang: number[] = [];
    for (const projet of this.projectsService.projects()) {
      const demarres = sprints
        .filter((s) => s.projectId === projet.id && s.statut !== 'planifie')
        .sort((a, b) => b.dateDebut.localeCompare(a.dateDebut))
        .slice(0, 6);
      demarres.forEach((s, rang) => (heuresParRang[rang] = (heuresParRang[rang] ?? 0) + (heuresParSprint.get(s.id) ?? 0)));
    }
    return heuresParRang.map((heures, rang) => ({ sprint: `S-${rang + 1}`, heures, courant: rang === 0 })).reverse();
  });
  protected readonly maxVelocity = computed(() => Math.max(1, ...this.velocity().map((v) => v.heures)));

  protected readonly projetsActifs = this.projectsService.totalActifs;
  protected readonly avancementMoyen = computed(() => {
    const projets = this.projectsService.projects();
    return projets.length > 0 ? Math.round(projets.reduce((sum, p) => sum + p.avancement, 0) / projets.length) : 0;
  });
  protected readonly projetsARisque = computed(() => this.projectsService.projects().filter((p) => p.sante !== 'sain').length);
  protected readonly jalonsARisque = computed(() => this.milestonesService.milestones().filter((m) => m.statut === 'a_risque').length);

  protected readonly attentionRequise = this.projectsService.attentionRequise();

  constructor() {
    effect(() => this.projectsService.chargerIndicateurs());
  }

  responsableNom(id: string): string {
    return this.peopleService.byId(id)?.nom ?? '—';
  }
}
