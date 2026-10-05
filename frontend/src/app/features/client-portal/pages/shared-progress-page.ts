import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { ProjectsService } from '@core/services/projects.service';
import { MilestonesService } from '@core/services/milestones.service';
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
  private readonly partagesClientService = inject(PartagesClientService);

  protected readonly santeConfig = SANTE_CONFIG;
  protected readonly formatDateFr = formatDateFr;
  protected readonly formatPercent = formatPercent;

  private readonly clientId = computed(() => this.authService.currentUser()?.id ?? '');

  protected readonly projetsPartages = computed(() => {
    const shares = this.partagesClientService.activeSharesForClient(this.clientId())();
    return shares.map((s) => this.projectsService.byId(s.projectId)()).filter((p) => p !== undefined);
  });

  protected prochaineEtape(projetId: string): string {
    const jalon = this.milestonesService
      .byProject(projetId)()
      .find((m) => m.statut !== 'atteint');
    return jalon ? `${jalon.nom} — ${formatDateFr(jalon.date)}` : 'Aucune étape en attente';
  }
}
