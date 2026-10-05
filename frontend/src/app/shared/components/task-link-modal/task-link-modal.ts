import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { Task, TypeLienDependance } from '@core/models/task.model';
import { TasksService } from '@core/services/tasks.service';
import { Modal } from '@shared/components/modal/modal';
import { StatusBadge } from '@shared/components/status-badge/status-badge';

const LIEN_LABELS: Record<TypeLienDependance, { titre: string; code: string; astuce: string }> = {
  FD: { titre: 'Fin → Début', code: 'FIN_DEBUT', astuce: 'Le cas courant : la suivante attend la fin de la précédente.' },
  DD: { titre: 'Début → Début', code: 'DEBUT_DEBUT', astuce: 'Les deux tâches démarrent ensemble.' },
  FF: { titre: 'Fin → Fin', code: 'FIN_FIN', astuce: 'Les deux tâches doivent se terminer ensemble.' },
  DF: { titre: 'Début → Fin', code: 'DEBUT_FIN', astuce: 'La suivante doit se terminer après le démarrage de la précédente.' },
};

@Component({
  selector: 'app-task-link-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Modal, StatusBadge],
  templateUrl: './task-link-modal.html',
  styleUrl: './task-link-modal.css',
})
export class TaskLinkModal {
  readonly open = input.required<boolean>();
  readonly taskId = input<string | null>(null);
  readonly close = output<void>();

  private readonly tasksService = inject(TasksService);

  protected readonly lienLabels = LIEN_LABELS;
  protected readonly typesLien: TypeLienDependance[] = ['FD', 'DD', 'FF', 'DF'];
  protected readonly sensLien = signal<'bloquee_par' | 'bloque'>('bloque');
  protected readonly rechercheTache = signal('');
  protected readonly tacheLieeId = signal<string | null>(null);
  protected readonly typeLienChoisi = signal<TypeLienDependance>('FD');

  protected readonly task = computed(() => {
    const id = this.taskId();
    return id ? this.tasksService.byId(id)() : undefined;
  });

  private readonly dejaLieesIds = computed(() => {
    const t = this.task();
    if (!t) return new Set<string>();
    const deps = this.tasksService.dependances();
    return new Set(
      deps.filter((d) => d.idTacheSource === t.id || d.idTacheCible === t.id).map((d) => (d.idTacheSource === t.id ? d.idTacheCible : d.idTacheSource)),
    );
  });

  protected readonly candidatsLien = computed(() => {
    const t = this.task();
    if (!t) return [];
    const recherche = this.rechercheTache().trim().toLowerCase();
    const dejaLiees = this.dejaLieesIds();
    return this.tasksService
      .byProject(t.projectId)()
      .filter((c) => c.id !== t.id)
      .filter((c) => !recherche || c.code.toLowerCase().includes(recherche) || c.titre.toLowerCase().includes(recherche))
      .map((c) => ({ tache: c, dejaLiee: dejaLiees.has(c.id) }));
  });

  protected readonly tacheLieeChoisie = computed(() => {
    const id = this.tacheLieeId();
    return id ? this.tasksService.byId(id)() : undefined;
  });

  protected readonly resumeLien = computed(() => {
    const t = this.task();
    const autre = this.tacheLieeChoisie();
    if (!t || !autre) return null;

    const source = this.sensLien() === 'bloque' ? t : autre;
    const cible = this.sensLien() === 'bloque' ? autre : t;
    const type = this.typeLienChoisi();
    const cibleRef = cible.id === t.id ? 'Cette tâche' : cible.code;
    const sourceRef = source.id === t.id ? 'cette tâche' : source.code;
    const verbe = type === 'FD' || type === 'DD' ? 'démarrer' : 'se terminer';
    const declencheur = type === 'FD' || type === 'DF' ? 'la fin' : 'le démarrage';
    const phrase = `${cibleRef} ne pourra ${verbe} qu'après ${declencheur} de ${sourceRef}.`;

    return { source, cible, type, phrase, autreTitre: autre.titre } satisfies {
      source: Task;
      cible: Task;
      type: TypeLienDependance;
      phrase: string;
      autreTitre: string;
    };
  });

  constructor() {
    effect(() => {
      if (this.open()) {
        this.sensLien.set('bloque');
        this.rechercheTache.set('');
        this.tacheLieeId.set(null);
        this.typeLienChoisi.set('FD');
      }
    });
  }

  choisirSensLien(sens: 'bloquee_par' | 'bloque'): void {
    this.sensLien.set(sens);
  }

  choisirTacheLiee(taskId: string, dejaLiee: boolean): void {
    if (dejaLiee) return;
    this.tacheLieeId.set(taskId);
  }

  choisirTypeLien(type: TypeLienDependance): void {
    this.typeLienChoisi.set(type);
  }

  async creerLien(): Promise<void> {
    const resume = this.resumeLien();
    if (!resume) return;
    try {
      await this.tasksService.creerDependance(resume.source.id, resume.cible.id, resume.type);
      this.close.emit();
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }
}
