import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { TasksService } from '@core/services/tasks.service';
import { ProjectsService } from '@core/services/projects.service';
import { SprintsService } from '@core/services/sprints.service';
import { AuthService } from '@core/services/auth.service';
import { HorlogeService } from '@core/services/horloge.service';
import { StatutTache, Task } from '@core/models/task.model';
import { StatusBadge } from '@shared/components/status-badge/status-badge';
import { ProgressBar } from '@shared/components/progress-bar/progress-bar';
import { ToastService } from '@shared/components/toast/toast.service';
import { formatHours, formatHms } from '@shared/utils/format.util';
import { tempsPasseActuel } from '@shared/utils/temps-passe.util';

@Component({
  selector: 'app-time-tracking-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [StatusBadge, ProgressBar],
  templateUrl: './time-tracking-page.html',
  styleUrl: './time-tracking-page.css',
})
export class TimeTrackingPage {
  private readonly tasksService = inject(TasksService);
  private readonly projectsService = inject(ProjectsService);
  private readonly sprintsService = inject(SprintsService);
  private readonly authService = inject(AuthService);
  private readonly horlogeService = inject(HorlogeService);
  private readonly toast = inject(ToastService);

  protected readonly formatHours = formatHours;
  protected readonly formatHms = formatHms;

  protected readonly mesProjets = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return [];
    return this.projectsService.pourUtilisateur(user, this.tasksService.tasks());
  });

  private readonly projetFiltreManuel = signal<string>('toutes');
  protected readonly projetFiltre = computed(() => this.projetFiltreManuel());

  /** Sprints du seul projet filtré, ou de tous les projets de l'utilisateur si aucun projet n'est choisi. */
  protected readonly sprintsDisponibles = computed(() => {
    const projetId = this.projetFiltre();
    const projets = projetId === 'toutes' ? this.mesProjets() : this.mesProjets().filter((p) => p.id === projetId);
    return projets.flatMap((p) => this.sprintsService.byProject(p.id)());
  });

  private readonly sprintParId = computed(() => new Map(this.sprintsDisponibles().map((s) => [s.id, s])));

  private readonly sprintFiltreManuel = signal<string>('toutes');
  protected readonly sprintFiltre = computed(() => this.sprintFiltreManuel());

  private readonly statutFiltreManuel = signal<StatutTache | 'toutes'>('toutes');
  protected readonly statutFiltre = computed(() => this.statutFiltreManuel());

  protected readonly mesTaches = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return [];
    const projetId = this.projetFiltre();
    const sprintId = this.sprintFiltre();
    const statut = this.statutFiltre();
    const sprints = this.sprintParId();

    const taches = this.tasksService
      .tasks()
      .filter((t) => t.assigneIds.includes(user.id))
      .filter((t) => projetId === 'toutes' || t.projectId === projetId)
      .filter((t) => sprintId === 'toutes' || t.sprintId === sprintId)
      .filter((t) => statut === 'toutes' || t.statut === statut)
      // Seules les tâches d'un sprint déjà commencé (en cours ou clôturé) ont leur place dans la saisie de temps.
      .filter((t) => t.sprintId !== undefined && sprints.get(t.sprintId)?.statut !== 'planifie')
      .sort((a, b) => a.code.localeCompare(b.code, undefined, { numeric: true }));
    // Charge les dépendances de chaque tâche affichée, pour l'indicateur de blocage du tableau.
    for (const t of taches) this.tasksService.ensureDetailLoaded(t.id);
    return taches;
  });

  changerProjetFiltre(value: string): void {
    this.projetFiltreManuel.set(value);
    this.sprintFiltreManuel.set('toutes');
  }

  changerSprintFiltre(value: string): void {
    this.sprintFiltreManuel.set(value);
  }

  changerStatutFiltre(value: string): void {
    this.statutFiltreManuel.set(value as StatutTache | 'toutes');
  }

  /** Temps passé « en direct » d'une tâche assignée — avance tout seul tant que son chronomètre tourne. */
  protected tempsPasseLive(task: Task): number {
    return tempsPasseActuel(task, this.horlogeService.maintenant());
  }

  /** Texte de blocage à afficher à côté du titre — null si aucune dépendance n'implique cette tâche. */
  protected texteBlocage(task: Task): string | null {
    const bloquantes = this.tasksService
      .dependancesBloquantes(task.id)()
      .map((dep) => this.tasksService.byId(dep.idTacheSource)())
      .filter((t): t is Task => t !== undefined);
    const bloquees = this.tasksService
      .dependancesBloquees(task.id)()
      .map((dep) => this.tasksService.byId(dep.idTacheCible)())
      .filter((t): t is Task => t !== undefined);

    const morceaux: string[] = [];
    if (bloquantes.length > 0) morceaux.push(`bloquée par ${bloquantes.map((t) => t.code).join(', ')}`);
    if (bloquees.length > 0) morceaux.push(`bloque ${bloquees.map((t) => t.code).join(', ')}`);
    return morceaux.length > 0 ? morceaux.join(' · ') : null;
  }

  protected readonly totalEstime = computed(() => this.mesTaches().reduce((s, t) => s + t.estimeHeures, 0));
  protected readonly totalPasse = computed(() => {
    const maintenant = this.horlogeService.maintenant();
    return this.mesTaches().reduce((s, t) => s + tempsPasseActuel(t, maintenant), 0);
  });

  protected readonly ecartGlobalStyle = computed(() => {
    const ecart = this.totalPasse() - this.totalEstime();
    return ecart > 0 ? 'time-tracking__ecart--depasse' : ecart < 0 ? 'time-tracking__ecart--sous' : '';
  });

  projetNom(projectId: string): string {
    return this.projectsService.byId(projectId)()?.nom ?? '';
  }

  ecart(estime: number, passe: number): number {
    return passe - estime;
  }

  ecartStyle(estime: number, passe: number): string {
    const e = this.ecart(estime, passe);
    return e > 0 ? 'time-tracking__ecart--depasse' : e < 0 ? 'time-tracking__ecart--sous' : 'time-tracking__ecart--neutre';
  }

  avancementPct(estime: number, passe: number): number {
    if (estime <= 0) return 0;
    return Math.min(999, Math.round((passe / estime) * 100));
  }

  avancementCouleur(estime: number, passe: number): string {
    const pct = this.avancementPct(estime, passe);
    if (pct > 100) return 'var(--color-error)';
    if (pct >= 80) return 'var(--color-alert)';
    return 'var(--color-success)';
  }

  actionLabel(statut: StatutTache): string {
    if (statut === 'a_faire') return 'Démarrer';
    if (statut === 'terminee') return 'Terminé';
    return 'Terminer';
  }

  actionDisabled(statut: StatutTache): boolean {
    return statut === 'terminee';
  }

  async agirSurTache(taskId: string, statut: StatutTache): Promise<void> {
    const tache = this.mesTaches().find((t) => t.id === taskId);
    if (!tache) return;
    try {
      if (statut === 'a_faire') {
        await this.tasksService.updateStatus(taskId, 'en_cours');
        this.toast.show({ type: 'info', message: `${tache.code} démarrée — le temps passé se met à jour automatiquement.` });
        return;
      }
      if (statut !== 'terminee') {
        await this.tasksService.updateStatus(taskId, 'terminee');
        this.toast.show({ type: 'success', message: `${tache.code} terminée.` });
      }
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }
}
