import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { Task } from '@core/models/task.model';
import { PeopleService } from '@core/services/people.service';
import { TasksService } from '@core/services/tasks.service';
import { HorlogeService } from '@core/services/horloge.service';
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
  readonly open = output<string>();
  readonly dragStart = output<string>();
  readonly edit = output<string>();
  readonly delete = output<string>();

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

  protected readonly formatDateFr = formatDateFr;
  protected readonly formatHms = formatHms;

  /** Temps passé « en direct » — avance tout seul tant que le chronomètre de la tâche tourne. */
  protected readonly tempsPasseLive = computed(() => tempsPasseActuel(this.task(), this.horlogeService.maintenant()));

  protected subTasksDoneLabel(): string {
    const sousTaches = this.task().sousTaches;
    return `${sousTaches.filter((s) => s.fait).length} / ${sousTaches.length}`;
  }
}
