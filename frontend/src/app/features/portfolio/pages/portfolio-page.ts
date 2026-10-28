import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ProjectsService } from '@core/services/projects.service';
import { PeopleService } from '@core/services/people.service';
import { AuthService } from '@core/services/auth.service';
import { Role } from '@core/models/role.model';
import { SanteProjet, StatutProjet } from '@core/models/project.model';
import { ProjectCard } from '@shared/components/project-card/project-card';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { SearchBar } from '@shared/components/search-bar/search-bar';

type FiltreSante = 'tous' | SanteProjet;
type FiltreStatut = 'tous' | StatutProjet;

const STATUT_FILTRE_LABEL: Record<FiltreStatut, string> = {
  tous: 'Tous statuts',
  en_preparation: 'En préparation',
  en_cours: 'En cours',
  suspendu: 'Suspendu',
  cloture: 'Clôturé',
  archive: 'Archivé',
};

const STATUT_STYLE: Record<StatutProjet, { label: string; bg: string; text: string }> = {
  en_preparation: { label: 'En préparation', bg: 'var(--status-a-faire-bg)', text: 'var(--status-a-faire-text)' },
  en_cours: { label: 'En cours', bg: 'var(--status-en-cours-bg)', text: 'var(--status-en-cours-text)' },
  suspendu: { label: 'Suspendu', bg: 'var(--priority-haute-bg)', text: 'var(--priority-haute-text)' },
  cloture: { label: 'Clôturé', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)' },
  archive: { label: 'Archivé', bg: 'var(--color-surface-alt)', text: 'var(--color-text-tertiary)' },
};

@Component({
  selector: 'app-portfolio-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ProjectCard, EmptyState, SearchBar],
  templateUrl: './portfolio-page.html',
  styleUrl: './portfolio-page.css',
})
export class PortfolioPage {
  private readonly projectsService = inject(ProjectsService);
  private readonly peopleService = inject(PeopleService);
  private readonly authService = inject(AuthService);

  protected readonly recherche = signal('');
  protected readonly filtre = signal<FiltreSante>('tous');
  protected readonly statutFiltre = signal<FiltreStatut>('tous');
  protected readonly statutFiltreLabel = STATUT_FILTRE_LABEL;
  protected readonly statutsDisponibles: FiltreStatut[] = ['tous', 'en_preparation', 'en_cours', 'suspendu', 'cloture', 'archive'];
  protected readonly cartesCliquables = computed(() => this.authService.currentUser()?.role !== Role.DirecteurDsi);
  protected readonly statutStyle = STATUT_STYLE;

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
    const statutFiltre = this.statutFiltre();
    const terme = this.recherche().trim().toLowerCase();
    return [...projets]
      .filter((p) => filtre === 'tous' || p.sante === filtre)
      .filter((p) => statutFiltre === 'tous' || p.statut === statutFiltre)
      .filter((p) => !terme || p.nom.toLowerCase().includes(terme))
      .sort((a, b) => this.criticiteScore[b.sante] - this.criticiteScore[a.sante]);
  });

  responsable(id: string) {
    return this.peopleService.byId(id);
  }

  setFiltre(value: FiltreSante): void {
    this.filtre.set(value);
  }

  changerStatutFiltre(value: string): void {
    this.statutFiltre.set(value as FiltreStatut);
  }
}
