import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { ProjectsService } from '@core/services/projects.service';
import { MilestonesService } from '@core/services/milestones.service';
import { SprintsService } from '@core/services/sprints.service';
import { PartagesClientService } from '@core/services/partages-client.service';
import { SANTE_CONFIG } from '@shared/utils/sante-config';
import { formatDateFr, formatPercent } from '@shared/utils/format.util';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { ProgressBar } from '@shared/components/progress-bar/progress-bar';

@Component({
  selector: 'app-shared-progress-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState, ProgressBar],
  templateUrl: './shared-progress-page.html',
  styleUrl: './shared-progress-page.css',
})
export class SharedProgressPage {
  private readonly authService = inject(AuthService);
  private readonly projectsService = inject(ProjectsService);
  private readonly milestonesService = inject(MilestonesService);
  private readonly sprintsService = inject(SprintsService);
  private readonly partagesClientService = inject(PartagesClientService);

  protected readonly santeConfig = SANTE_CONFIG;
  protected readonly formatDateFr = formatDateFr;
  protected readonly formatPercent = formatPercent;

  private readonly clientId = computed(() => this.authService.currentUser()?.id ?? '');

  protected readonly projetsPartages = computed(() => {
    const shares = this.partagesClientService.activeSharesForClient(this.clientId())();
    return shares.map((s) => this.projectsService.byId(s.projectId)()).filter((p) => p !== undefined);
  });

  constructor() {
    // Sprint affiché sur chaque carte ("Sprint X sur Y") et prochaine étape : indicateurs calculés côté
    // frontend à partir des sprints et jalons du projet, jamais chargés sans cet appel explicite (comme
    // dans Mes projets). Les tâches ne sont volontairement pas chargées : cet espace est en lecture seule
    // sur l'avancement, sans accès au détail des tâches (voir le bandeau d'information de la page).
    effect(() => {
      for (const projet of this.projetsPartages()) {
        this.sprintsService.ensureLoaded(projet.id);
        this.milestonesService.ensureLoaded(projet.id);
      }
    });
  }

  protected prochaineEtape(projetId: string): string {
    const jalon = this.milestonesService
      .byProject(projetId)()
      .find((m) => m.statut !== 'atteint');
    return jalon ? `${jalon.nom} — ${formatDateFr(jalon.date)}` : 'Aucune étape en attente';
  }
}
