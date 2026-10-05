import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { ProjectsService } from '@core/services/projects.service';
import { SprintsService } from '@core/services/sprints.service';
import { MilestonesService } from '@core/services/milestones.service';
import { AiSuggestionChip } from '@shared/components/ai-suggestion-chip/ai-suggestion-chip';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { formatDateCourteFr } from '@shared/utils/format.util';

type Zoom = 'mois' | 'semaines' | 'trimestres';

interface GanttRow {
  libelle: string;
  isPhase: boolean;
  startPct: number;
  widthPct: number;
  progressPct: number;
  dependOnEndPct: number | null;
}

@Component({
  selector: 'app-gantt-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AiSuggestionChip, EmptyState, TitleCasePipe],
  templateUrl: './gantt-page.html',
  styleUrl: './gantt-page.css',
})
export class GanttPage {
  readonly projectId = input.required<string>();

  private readonly projectsService = inject(ProjectsService);
  private readonly sprintsService = inject(SprintsService);
  private readonly milestonesService = inject(MilestonesService);

  protected readonly zoom = signal<Zoom>('mois');
  protected readonly afficherDependances = signal(true);

  protected readonly project = computed(() => this.projectsService.byId(this.projectId())());

  private readonly rangeMs = computed(() => {
    const p = this.project();
    if (!p) return { start: 0, end: 1 };
    return { start: new Date(p.dateDebut).getTime(), end: new Date(p.dateFin).getTime() };
  });

  private pct(dateIso: string): number {
    const { start, end } = this.rangeMs();
    const total = end - start;
    if (total <= 0) return 0;
    return Math.max(0, Math.min(100, ((new Date(dateIso).getTime() - start) / total) * 100));
  }

  protected readonly periodHeader = computed(() => {
    const p = this.project();
    if (!p) return [];
    const start = new Date(p.dateDebut);
    const end = new Date(p.dateFin);
    const zoom = this.zoom();

    if (zoom === 'semaines') {
      const labels: string[] = [];
      const cursor = new Date(start);
      while (cursor <= end) {
        labels.push(formatDateCourteFr(cursor.toISOString()));
        cursor.setDate(cursor.getDate() + 7);
      }
      return labels;
    }

    if (zoom === 'trimestres') {
      const labels: string[] = [];
      const cursor = new Date(start.getFullYear(), Math.floor(start.getMonth() / 3) * 3, 1);
      while (cursor <= end) {
        labels.push(`T${Math.floor(cursor.getMonth() / 3) + 1} ${cursor.getFullYear()}`);
        cursor.setMonth(cursor.getMonth() + 3);
      }
      return labels;
    }

    const labels: string[] = [];
    const cursor = new Date(start.getFullYear(), start.getMonth(), 1);
    while (cursor <= end) {
      labels.push(cursor.toLocaleDateString('fr-FR', { month: 'short' }));
      cursor.setMonth(cursor.getMonth() + 1);
    }
    return labels;
  });

  protected readonly aujourdhuiPct = computed(() => {
    const now = Date.now();
    const { start, end } = this.rangeMs();
    if (now < start || now > end) return null;
    return this.pct(new Date(now).toISOString());
  });

  protected readonly rows = computed<GanttRow[]>(() => {
    const sprints = [...this.sprintsService.byProject(this.projectId())()].sort(
      (a, b) => new Date(a.dateDebut).getTime() - new Date(b.dateDebut).getTime(),
    );
    return sprints.map((s, i) => {
      const startPct = this.pct(s.dateDebut);
      const precedent = i > 0 ? sprints[i - 1] : undefined;
      const precedentEndPct = precedent ? this.pct(precedent.dateFin) : null;
      return {
        libelle: s.nom,
        isPhase: false,
        startPct,
        widthPct: Math.max(2, this.pct(s.dateFin) - startPct),
        progressPct: s.statut === 'cloture' ? 100 : s.statut === 'en_cours' ? (this.project()?.avancement ?? 0) : 0,
        dependOnEndPct: precedentEndPct !== null && precedentEndPct < startPct ? precedentEndPct : null,
      };
    });
  });

  protected readonly milestoneMarkers = computed(() =>
    this.milestonesService
      .byProject(this.projectId())()
      .map((m) => ({ nom: m.nom, positionPct: this.pct(m.date), couleur: m.statut === 'a_risque' ? 'var(--color-error)' : 'var(--color-ai)' })),
  );
}
