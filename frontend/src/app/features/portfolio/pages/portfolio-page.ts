import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ProjectsService } from '@core/services/projects.service';
import { PeopleService } from '@core/services/people.service';
import { AuthService } from '@core/services/auth.service';
import { Role } from '@core/models/role.model';
import { SanteProjet } from '@core/models/project.model';
import { ProjectCard } from '@shared/components/project-card/project-card';
import { EmptyState } from '@shared/components/empty-state/empty-state';

type FiltreSante = 'tous' | SanteProjet;

@Component({
  selector: 'app-portfolio-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ProjectCard, EmptyState],
  templateUrl: './portfolio-page.html',
  styleUrl: './portfolio-page.css',
})
export class PortfolioPage {
  private readonly projectsService = inject(ProjectsService);
  private readonly peopleService = inject(PeopleService);
  private readonly authService = inject(AuthService);

  protected readonly filtre = signal<FiltreSante>('tous');
  protected readonly cartesCliquables = computed(() => this.authService.currentUser()?.role !== Role.DirecteurDsi);

  protected readonly compteurs = computed(() => {
    const projets = this.projectsService.projects();
    return {
      tous: projets.length,
      a_risque: projets.filter((p) => p.sante === 'a_risque').length,
      sous_surveillance: projets.filter((p) => p.sante === 'sous_surveillance').length,
      sain: projets.filter((p) => p.sante === 'sain').length,
    };
  });

  protected readonly criticiteScore: Record<SanteProjet, number> = { a_risque: 2, sous_surveillance: 1, sain: 0 };

  protected readonly projetsFiltres = computed(() => {
    const projets = this.projectsService.projects();
    const filtre = this.filtre();
    return [...projets]
      .filter((p) => filtre === 'tous' || p.sante === filtre)
      .sort((a, b) => this.criticiteScore[b.sante] - this.criticiteScore[a.sante]);
  });

  responsable(id: string) {
    return this.peopleService.byId(id);
  }

  setFiltre(value: FiltreSante): void {
    this.filtre.set(value);
  }
}
