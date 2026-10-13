import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { ProjectsService } from '@core/services/projects.service';
import { MilestonesService } from '@core/services/milestones.service';
import { PartagesClientService } from '@core/services/partages-client.service';
import { formatDateFr } from '@shared/utils/format.util';
import { EmptyState } from '@shared/components/empty-state/empty-state';

const JALON_STYLE: Record<string, { label: string; bg: string; text: string; icone: string }> = {
  atteint: { label: 'Terminé', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)', icone: '✓' },
  a_risque: { label: 'En cours', bg: 'var(--status-en-cours-bg)', text: 'var(--status-en-cours-text)', icone: '◉' },
  planifie: { label: 'À venir', bg: 'var(--status-a-faire-bg)', text: 'var(--status-a-faire-text)', icone: '◇' },
  annule: { label: 'Annulé', bg: 'var(--color-surface-alt)', text: 'var(--color-text-tertiary)', icone: '✕' },
};

@Component({
  selector: 'app-key-steps-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState],
  templateUrl: './key-steps-page.html',
  styleUrl: './key-steps-page.css',
})
export class KeyStepsPage {
  private readonly authService = inject(AuthService);
  private readonly projectsService = inject(ProjectsService);
  private readonly milestonesService = inject(MilestonesService);
  private readonly partagesClientService = inject(PartagesClientService);

  protected readonly jalonStyle = JALON_STYLE;
  protected readonly formatDateFr = formatDateFr;

  private readonly clientId = computed(() => this.authService.currentUser()?.id ?? '');

  protected readonly projetsPartages = computed(() => {
    const shares = this.partagesClientService.activeSharesForClient(this.clientId())();
    return shares.map((s) => this.projectsService.byId(s.projectId)()).filter((p) => p !== undefined);
  });

  /** Projet choisi dans la combobox (utile quand plusieurs projets sont partagés) — le premier par défaut. */
  protected readonly projetSelectionneId = signal('');

  constructor() {
    effect(() => {
      const projets = this.projetsPartages();
      if (projets.length > 0 && !projets.some((p) => p.id === this.projetSelectionneId())) {
        this.projetSelectionneId.set(projets[0].id);
      }
    });
  }

  changerProjet(id: string): void {
    this.projetSelectionneId.set(id);
  }

  protected readonly projetEtapes = computed(() => {
    const projet = this.projetsPartages().find((p) => p.id === this.projetSelectionneId());
    return projet ? { projet, etapes: this.milestonesService.byProject(projet.id)() } : undefined;
  });
}
