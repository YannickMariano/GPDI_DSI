import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { ProjectsService } from '@core/services/projects.service';
import { PartagesClientService } from '@core/services/partages-client.service';
import { PointsInformationService } from '@core/services/points-information.service';
import { formatDateFr } from '@shared/utils/format.util';
import { EmptyState } from '@shared/components/empty-state/empty-state';

@Component({
  selector: 'app-info-points-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState],
  templateUrl: './info-points-page.html',
  styleUrl: './info-points-page.css',
})
export class InfoPointsPage {
  private readonly authService = inject(AuthService);
  private readonly projectsService = inject(ProjectsService);
  private readonly partagesClientService = inject(PartagesClientService);
  private readonly pointsInformationService = inject(PointsInformationService);

  protected readonly formatDateFr = formatDateFr;

  private readonly clientId = computed(() => this.authService.currentUser()?.id ?? '');

  private readonly projetsPartages = computed(() => {
    const shares = this.partagesClientService.activeSharesForClient(this.clientId())();
    return shares.map((s) => this.projectsService.byId(s.projectId)()).filter((p) => p !== undefined);
  });

  protected readonly projetsPoints = computed(() =>
    this.projetsPartages().map((projet) => ({
      projet,
      points: this.pointsInformationService.byProject(projet.id)(),
    })),
  );
}
