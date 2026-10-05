import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { ProjectsService } from '@core/services/projects.service';
import { RisquesProjetService } from '@core/services/risques-projet.service';
import { PeopleService } from '@core/services/people.service';
import { RiskMatrix } from '@shared/components/risk-matrix/risk-matrix';
import { RiskTable } from '@shared/components/risk-table/risk-table';

@Component({
  selector: 'app-risks-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RiskMatrix, RiskTable],
  templateUrl: './risks-page.html',
  styleUrl: './risks-page.css',
})
export class RisksPage {
  private readonly projectsService = inject(ProjectsService);
  private readonly risquesService = inject(RisquesProjetService);
  private readonly peopleService = inject(PeopleService);

  /** Risques actifs de l'ensemble des projets (un risque clos n'est plus suivi). */
  protected readonly risks = computed(() => this.risquesService.risques().filter((r) => r.statut !== 'clos'));
  protected readonly people = this.peopleService.people;

  constructor() {
    effect(() => {
      for (const id of this.projectsService.projectIds()) this.risquesService.ensureLoaded(id);
    });
  }
}
