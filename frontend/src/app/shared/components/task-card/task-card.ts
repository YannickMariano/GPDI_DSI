import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { Task } from '@core/models/task.model';
import { PeopleService } from '@core/services/people.service';
import { TasksService } from '@core/services/tasks.service';
import { HorlogeService } from '@core/services/horloge.service';
import { SprintsService } from '@core/services/sprints.service';
import { PriorityBadge } from '@shared/components/priority-badge/priority-badge';
import { AvatarGroup } from '@shared/components/avatar/avatar-group';
import { formatDateFr, formatHms } from '@shared/utils/format.util';
import { tempsPasseActuel } from '@shared/utils/temps-passe.util';

@Component({
  selector: 'app-task-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PriorityBadge, AvatarGroup],
  host: {
    class: 'task-card',
    '[attr.draggable]': 'draggable() ? "true" : null',
    '(dragstart)': 'draggable() && dragStart.emit(task().id)',
  },
  templateUrl: './task-card.html',
  styleUrl: './task-card.css',
})
export class TaskCard {
  readonly task = input.required<Task>();
  readonly draggable = input<boolean>(false);
  readonly canManage = input<boolean>(false);
  /** Affiche l'action « Archiver » (avec canManage) ; elle n'apparaît que sur une tâche terminée. */
  readonly archivable = input<boolean>(false);
  readonly open = output<string>();
  readonly dragStart = output<string>();
  readonly edit = output<string>();
  readonly delete = output<string>();
  readonly archive = output<string>();

  protected readonly peutArchiver = computed(() => this.canManage() && this.archivable() && this.task().statut === 'terminee');

  onArchive(event: Event): void {
    event.stopPropagation();
    this.archive.emit(this.task().id);
  }

  onEdit(event: Event): void {
    event.stopPropagation();
    this.edit.emit(this.task().id);
  }

  onDelete(event: Event): void {
    event.stopPropagation();
    this.delete.emit(this.task().id);
  }

  private readonly peopleService = inject(PeopleService);
  private readonly tasksService = inject(TasksService);
  private readonly horlogeService = inject(HorlogeService);
  private readonly sprintsService = inject(SprintsService);

  protected readonly assignes = computed(() =>
    this.task()
      .assigneIds.map((id) => this.peopleService.byId(id))
      .filter((p) => p !== undefined)
      .map((p) => ({ initiales: p.initiales, bg: p.avatarBg, nom: p.nom })),
  );

  /** Tâche préalable non terminée qui bloque celle-ci — dérivée des vraies dépendances (visible si déjà chargées). */
  protected readonly tacheBloquante = computed(() => {
    const t = this.task();
    return this.tasksService
      .dependancesBloquantes(t.id)()
      .map((dep) => this.tasksService.byId(dep.idTacheSource)())
      .find((tache): tache is Task => tache !== undefined && tache.statut !== 'terminee' && tache.statut !== 'reportee');
  });

  /** Tâche pas encore terminée alors que son sprint a dépassé sa date de fin (une tâche terminée en retard est marquée par depasseSprint). */
  protected readonly enRetardSurSprint = computed(() => {
    const t = this.task();
    if (t.statut === 'terminee' || t.statut === 'reportee' || !t.sprintId) return false;
    const sprint = this.sprintsService.byId(t.sprintId)();
    return sprint !== undefined && sprint.dateFin < dateDuJour(this.horlogeService.maintenant());
  });

  protected readonly formatDateFr = formatDateFr;
  protected readonly formatHms = formatHms;

  /** Temps passé « en direct » — avance tout seul tant que le chronomètre de la tâche tourne. */
  protected readonly tempsPasseLive = computed(() => tempsPasseActuel(this.task(), this.horlogeService.maintenant()));

  protected subTasksDoneLabel(): string {
    const sousTaches = this.task().sousTaches;
    return `${sousTaches.filter((s) => s.fait).length} / ${sousTaches.length}`;
  }
}

/** Date (heure locale) au format AAAA-MM-JJ, comparable aux dates des sprints. */
function dateDuJour(horodatage: number): string {
  const date = new Date(horodatage);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
