import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { TasksService, KANBAN_COLUMNS } from '@core/services/tasks.service';
import { SprintsService } from '@core/services/sprints.service';
import { ProjectsService } from '@core/services/projects.service';
import { AuthService } from '@core/services/auth.service';
import { PeopleService } from '@core/services/people.service';
import { Role } from '@core/models/role.model';
import { Priorite, StatutTache } from '@core/models/task.model';
import { TaskCard } from '@shared/components/task-card/task-card';
import { TaskDetailPanel } from '@shared/components/task-detail-panel/task-detail-panel';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { ProgressBar } from '@shared/components/progress-bar/progress-bar';
import { formatSprintLabel, sprintAAfficher } from '@shared/utils/sprint-label.util';
import { SANTE_CONFIG } from '@shared/utils/sante-config';
import { ToastService } from '@shared/components/toast/toast.service';

@Component({
  selector: 'app-my-work-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TaskCard, TaskDetailPanel, EmptyState, ProgressBar],
  templateUrl: './my-work-page.html',
  styleUrl: './my-work-page.css',
})
export class MyWorkPage {
  private readonly tasksService = inject(TasksService);
  private readonly sprintsService = inject(SprintsService);
  private readonly projectsService = inject(ProjectsService);
  private readonly authService = inject(AuthService);
  private readonly peopleService = inject(PeopleService);
  private readonly toast = inject(ToastService);

  protected readonly columns = KANBAN_COLUMNS;
  protected readonly selectedTaskId = signal<string | null>(null);
  protected readonly filtrePriorite = signal<Priorite | 'toutes'>('toutes');
  protected readonly assigneAMoi = signal(false);
  protected readonly draggedTaskId = signal<string | null>(null);

  protected readonly mesProjets = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return [];
    return this.projectsService.pourUtilisateur(user, this.tasksService.tasks());
  });

  private readonly projetFiltreManuel = signal<string | null>(null);
  protected readonly projetFiltre = computed(() => {
    const projets = this.mesProjets();
    const manuel = this.projetFiltreManuel();
    if (manuel && projets.some((p) => p.id === manuel)) return manuel;
    const actif = this.authService.currentUser()?.projetActifId;
    if (actif && projets.some((p) => p.id === actif)) return actif;
    return projets[0]?.id ?? '';
  });

  protected readonly projetSelectionne = computed(() => this.projectsService.byId(this.projetFiltre())());

  protected readonly sante = computed(() => {
    const projet = this.projetSelectionne();
    return projet ? SANTE_CONFIG[projet.sante] : undefined;
  });

  protected readonly responsable = computed(() => {
    const projet = this.projetSelectionne();
    return projet ? this.peopleService.byId(projet.responsableId) : undefined;
  });

  protected readonly sprintsProjet = computed(() => {
    const projetId = this.projetFiltre();
    return projetId ? this.sprintsService.byProject(projetId)() : [];
  });

  protected readonly sprintLabel = computed(() => {
    const projet = this.projetSelectionne();
    return formatSprintLabel(sprintAAfficher(this.sprintsProjet()), projet?.sprintActuelIndex, projet?.sprintTotal);
  });

  private readonly sprintFiltreManuel = signal<string | null>(null);
  protected readonly sprintFiltre = computed(() => this.sprintFiltreManuel() ?? sprintAAfficher(this.sprintsProjet())?.id ?? 'toutes');

  protected readonly mesTaches = computed(() => {
    const projetId = this.projetFiltre();
    if (!projetId) return [];
    const toutesLesTachesDuProjet = this.tasksService.byProject(projetId)();
    const user = this.authService.currentUser();
    const sprint = this.sprintFiltre();
    const base = sprint === 'toutes' ? toutesLesTachesDuProjet : toutesLesTachesDuProjet.filter((t) => t.sprintId === sprint);
    // Un membre équipe ne voit que les tâches des sprints déjà commencés (en cours ou clôturés) ;
    // le chef de projet, lui, voit aussi le backlog et les sprints encore planifiés.
    const restreintAuxSprintsCommences = user?.role === Role.MembreEquipe;
    const sprintsParId = new Map(this.sprintsProjet().map((s) => [s.id, s]));
    return base
      .filter((t) => !restreintAuxSprintsCommences || (t.sprintId !== undefined && sprintsParId.get(t.sprintId)?.statut !== 'planifie'))
      .filter((t) => !this.assigneAMoi() || !user || t.assigneIds.includes(user.id))
      .filter((t) => this.filtrePriorite() === 'toutes' || t.priorite === this.filtrePriorite());
  });

  tachesParColonne(statut: (typeof KANBAN_COLUMNS)[number]['statut']) {
    return this.mesTaches().filter((t) => t.statut === statut);
  }

  onCardDragStart(taskId: string): void {
    this.draggedTaskId.set(taskId);
  }

  async onColumnDrop(statut: StatutTache): Promise<void> {
    const taskId = this.draggedTaskId();
    this.draggedTaskId.set(null);
    if (!taskId) return;
    const tache = this.mesTaches().find((t) => t.id === taskId);
    if (!tache || tache.statut === statut) return;
    const user = this.authService.currentUser();
    if (!user || !tache.assigneIds.includes(user.id)) {
      this.toast.show({ type: 'error', message: "Vous n'êtes pas affecté à cette tâche." });
      return;
    }
    try {
      await this.tasksService.updateStatus(taskId, statut);
      this.toast.show({ type: 'success', message: `${tache.code} déplacée vers « ${this.columns.find((c) => c.statut === statut)?.titre} ».` });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  openTask(taskId: string): void {
    this.selectedTaskId.set(taskId);
  }

  closePanel(): void {
    this.selectedTaskId.set(null);
  }

  changerProjetFiltre(value: string): void {
    this.projetFiltreManuel.set(value);
    this.sprintFiltreManuel.set(null);
  }

  changerSprintFiltre(value: string): void {
    this.sprintFiltreManuel.set(value);
  }

  toggleAssigneAMoi(): void {
    this.assigneAMoi.update((v) => !v);
  }
}
