import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TasksService, KANBAN_COLUMNS } from '@core/services/tasks.service';
import { SprintsService } from '@core/services/sprints.service';
import { PeopleService } from '@core/services/people.service';
import { ProjectsService } from '@core/services/projects.service';
import { EquipesService } from '@core/services/equipes.service';
import { AuthService } from '@core/services/auth.service';
import { Role } from '@core/models/role.model';
import { Priorite, StatutTache, Task } from '@core/models/task.model';
import { TaskCard } from '@shared/components/task-card/task-card';
import { TaskDetailPanel } from '@shared/components/task-detail-panel/task-detail-panel';
import { Modal } from '@shared/components/modal/modal';
import { ConfirmationDialog } from '@shared/components/confirmation-dialog/confirmation-dialog';
import { ToastService } from '@shared/components/toast/toast.service';
import { PriorityBadge } from '@shared/components/priority-badge/priority-badge';
import { formatDateFr } from '@shared/utils/format.util';

@Component({
  selector: 'app-kanban-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TaskCard, TaskDetailPanel, Modal, ConfirmationDialog, ReactiveFormsModule, PriorityBadge],
  templateUrl: './kanban-page.html',
  styleUrl: './kanban-page.css',
})
export class KanbanPage {
  readonly projectId = input.required<string>();

  private readonly fb = inject(FormBuilder);
  private readonly tasksService = inject(TasksService);
  private readonly sprintsService = inject(SprintsService);
  private readonly peopleService = inject(PeopleService);
  private readonly projectsService = inject(ProjectsService);
  private readonly equipesService = inject(EquipesService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly columns = KANBAN_COLUMNS;
  protected readonly peutGererTaches = computed(() => this.authService.currentUser()?.role === Role.ChefProjet);
  protected readonly currentSprint = computed(() => this.sprintsService.current(this.projectId())());
  protected readonly sprints = computed(() => this.sprintsService.byProject(this.projectId())());
  protected readonly assigneAMoi = signal(false);
  protected readonly filtrePriorite = signal<Priorite | 'toutes'>('toutes');
  protected readonly selectedTaskId = signal<string | null>(null);

  private readonly sprintFiltreManuel = signal<string | null>(null);
  protected readonly sprintFiltre = computed(() => this.sprintFiltreManuel() ?? this.currentSprint()?.id ?? 'toutes');

  protected readonly tachesSprint = computed(() => {
    const filtre = this.sprintFiltre();
    const user = this.authService.currentUser();
    const base = filtre === 'toutes' ? this.tasksService.byProject(this.projectId())() : this.tasksService.bySprint(filtre)();
    return base
      .filter((t) => !t.archivee)
      .filter((t) => !this.assigneAMoi() || !user || t.assigneIds.includes(user.id))
      .filter((t) => this.filtrePriorite() === 'toutes' || t.priorite === this.filtrePriorite());
  });

  // --- Archives : tâches terminées archivées, listées (et non en Kanban), filtrables par sprint ---

  protected readonly vueArchives = signal(false);
  /** 'toutes' = tous les sprints ensemble, 'backlog' = tâches sans sprint, sinon l'identifiant d'un sprint. */
  protected readonly archiveSprintFiltre = signal<string>('toutes');
  protected readonly tacheAArchiver = signal<Task | null>(null);

  private readonly toutesArchivees = computed(() => this.tasksService.byProject(this.projectId())().filter((t) => t.archivee));
  protected readonly nombreArchivees = computed(() => this.toutesArchivees().length);
  protected readonly tachesArchivees = computed(() => {
    const filtre = this.archiveSprintFiltre();
    return this.toutesArchivees()
      .filter((t) => filtre === 'toutes' || (filtre === 'backlog' ? !t.sprintId : t.sprintId === filtre))
      .sort((a, b) => (b.dateArchivage ?? '').localeCompare(a.dateArchivage ?? ''));
  });

  protected readonly formatDateFr = formatDateFr;

  nomSprint(sprintId: string | undefined): string {
    if (!sprintId) return 'Backlog';
    return this.sprints().find((s) => s.id === sprintId)?.nom ?? '—';
  }

  nomsAssignes(task: Task): string {
    const noms = task.assigneIds.map((id) => this.peopleService.byId(id)?.nom).filter((nom) => !!nom);
    return noms.length > 0 ? noms.join(', ') : '—';
  }

  afficherArchives(afficher: boolean): void {
    this.vueArchives.set(afficher);
  }

  protected readonly restaurationEnCours = signal<string | null>(null);

  async restaurer(task: Task): Promise<void> {
    this.restaurationEnCours.set(task.id);
    try {
      await this.tasksService.restaurer(task.id);
      this.toast.show({ type: 'success', message: `Tâche ${task.code} restaurée dans le Kanban.` });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    } finally {
      this.restaurationEnCours.set(null);
    }
  }

  demanderArchivage(task: Task): void {
    this.tacheAArchiver.set(task);
  }

  annulerArchivage(): void {
    this.tacheAArchiver.set(null);
  }

  async confirmerArchivage(): Promise<void> {
    const task = this.tacheAArchiver();
    if (!task) return;
    try {
      await this.tasksService.archiver(task.id);
      if (this.selectedTaskId() === task.id) this.selectedTaskId.set(null);
      this.toast.show({ type: 'success', message: `Tâche ${task.code} archivée.` });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    } finally {
      this.tacheAArchiver.set(null);
    }
  }

  protected readonly totalTaches = computed(() => this.tachesSprint().length);
  protected readonly totalBloquees = computed(() => this.tachesSprint().filter((t) => t.statut === 'bloquee').length);

  /** Membres assignables : l'équipe DSI réelle rattachée au projet (seule éligible côté backend pour une assignation de tâche). */
  protected readonly equipeProjet = computed(() => {
    const equipeId = this.projectsService.byId(this.projectId())()?.equipeId;
    const equipe = equipeId ? this.equipesService.teams().find((e) => e.id === equipeId) : undefined;
    const ids = equipe ? [equipe.chefId, ...equipe.membreIds] : [];
    return ids.map((id) => this.peopleService.byId(id)).filter((p) => p !== undefined);
  });

  protected readonly draggedTaskId = signal<string | null>(null);
  protected readonly modalOuvert = signal(false);
  protected readonly tacheEnEdition = signal<Task | null>(null);
  protected readonly modeEditionTache = computed(() => this.tacheEnEdition() !== null);
  protected readonly tacheASupprimer = signal<Task | null>(null);

  protected readonly assigneesSelectionnesModal = signal<string[]>([]);
  protected readonly agentAAffecterModal = signal('');
  protected readonly agentsDisponiblesModal = computed(() => {
    const selectionnes = this.assigneesSelectionnesModal();
    return this.equipeProjet().filter((m) => m !== undefined && !selectionnes.includes(m.id));
  });

  protected readonly form = this.fb.nonNullable.group({
    titre: ['', Validators.required],
    description: [''],
    priorite: this.fb.nonNullable.control<Priorite>('normale', Validators.required),
    sprintId: [''],
    echeance: [''],
    estimeHeures: this.fb.nonNullable.control(4, [Validators.required, Validators.min(1)]),
  });

  tachesParColonne(statut: (typeof KANBAN_COLUMNS)[number]['statut']) {
    return this.tachesSprint().filter((t) => t.statut === statut);
  }

  openTask(taskId: string): void {
    this.selectedTaskId.set(taskId);
  }

  closePanel(): void {
    this.selectedTaskId.set(null);
  }

  toggleAssigneAMoi(): void {
    this.assigneAMoi.update((v) => !v);
  }

  onCardDragStart(taskId: string): void {
    this.draggedTaskId.set(taskId);
  }

  async onColumnDrop(statut: StatutTache): Promise<void> {
    const taskId = this.draggedTaskId();
    this.draggedTaskId.set(null);
    if (!taskId) return;
    const tache = this.tachesSprint().find((t) => t.id === taskId);
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

  changerSprintFiltre(value: string): void {
    this.sprintFiltreManuel.set(value);
  }

  ouvrirCreationTache(): void {
    this.tacheEnEdition.set(null);
    this.form.reset({ titre: '', description: '', priorite: 'normale', sprintId: this.currentSprint()?.id ?? '', echeance: '', estimeHeures: 4 });
    this.assigneesSelectionnesModal.set([]);
    this.agentAAffecterModal.set('');
    this.modalOuvert.set(true);
  }

  ouvrirModificationTache(task: Task): void {
    this.tacheEnEdition.set(task);
    this.form.reset({
      titre: task.titre,
      description: task.description ?? '',
      priorite: task.priorite,
      sprintId: task.sprintId ?? '',
      echeance: task.echeance ?? '',
      estimeHeures: task.estimeHeures,
    });
    this.assigneesSelectionnesModal.set([...task.assigneIds]);
    this.agentAAffecterModal.set('');
    this.modalOuvert.set(true);
  }

  fermerCreationTache(): void {
    this.modalOuvert.set(false);
  }

  personneModal(id: string) {
    return this.peopleService.byId(id);
  }

  assignerDansModal(): void {
    const id = this.agentAAffecterModal();
    if (!id) return;
    this.assigneesSelectionnesModal.update((list) => (list.includes(id) ? list : [...list, id]));
    this.agentAAffecterModal.set('');
  }

  retirerAssignationModal(id: string): void {
    this.assigneesSelectionnesModal.update((list) => list.filter((x) => x !== id));
  }

  async enregistrerTache(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { titre, description, priorite, sprintId, echeance, estimeHeures } = this.form.getRawValue();
    const payload = {
      titre,
      description: description || undefined,
      priorite,
      assigneIds: this.assigneesSelectionnesModal(),
      sprintId: sprintId || undefined,
      echeance: echeance || undefined,
      estimeHeures,
    };

    const enEdition = this.tacheEnEdition();
    try {
      if (enEdition) {
        await this.tasksService.update(enEdition.id, payload);
        this.toast.show({ type: 'success', message: `Tâche ${enEdition.code} modifiée.` });
      } else {
        const tache = await this.tasksService.create({ projectId: this.projectId(), ...payload });
        this.toast.show({ type: 'success', message: `Tâche ${tache.code} créée.` });
      }
      this.modalOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  demanderSuppressionTache(task: Task): void {
    this.tacheASupprimer.set(task);
  }

  annulerSuppressionTache(): void {
    this.tacheASupprimer.set(null);
  }

  async confirmerSuppressionTache(): Promise<void> {
    const task = this.tacheASupprimer();
    if (!task) return;
    try {
      await this.tasksService.remove(task.id);
      if (this.selectedTaskId() === task.id) this.selectedTaskId.set(null);
      this.toast.show({ type: 'success', message: `Tâche ${task.code} supprimée.` });
    } finally {
      this.tacheASupprimer.set(null);
    }
  }
}
