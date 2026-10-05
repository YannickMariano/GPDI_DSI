import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { TasksService } from '@core/services/tasks.service';
import { PeopleService } from '@core/services/people.service';
import { ProjectsService } from '@core/services/projects.service';
import { EquipesService } from '@core/services/equipes.service';
import { HorlogeService } from '@core/services/horloge.service';
import { Role } from '@core/models/role.model';
import { Task } from '@core/models/task.model';
import { SidePanel } from '@shared/components/side-panel/side-panel';
import { StatusBadge } from '@shared/components/status-badge/status-badge';
import { PriorityBadge } from '@shared/components/priority-badge/priority-badge';
import { ProgressBar } from '@shared/components/progress-bar/progress-bar';
import { Avatar } from '@shared/components/avatar/avatar';
import { TaskLinkModal } from '@shared/components/task-link-modal/task-link-modal';
import { RelativeTimePipe } from '@shared/pipes/relative-time.pipe';
import { formatDateFr, formatHours, formatHms } from '@shared/utils/format.util';
import { tempsPasseActuel } from '@shared/utils/temps-passe.util';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-task-detail-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgTemplateOutlet, SidePanel, StatusBadge, PriorityBadge, ProgressBar, Avatar, TaskLinkModal, RelativeTimePipe],
  templateUrl: './task-detail-panel.html',
  styleUrl: './task-detail-panel.css',
})
export class TaskDetailPanel {
  readonly taskId = input<string | null>(null);
  readonly close = output<void>();
  readonly openTask = output<string>();

  private readonly tasksService = inject(TasksService);
  private readonly peopleService = inject(PeopleService);
  private readonly projectsService = inject(ProjectsService);
  private readonly equipesService = inject(EquipesService);
  private readonly horlogeService = inject(HorlogeService);
  private readonly authService = inject(AuthService);

  protected readonly commentaire = signal('');
  protected readonly nouvelleSousTache = signal('');
  protected readonly agentAAffecter = signal('');
  protected readonly fichierSelectionne = signal<File | null>(null);

  protected readonly modalLienOuvert = signal(false);

  protected readonly peutGererTache = computed(() => this.authService.currentUser()?.role === Role.ChefProjet);

  protected readonly task = computed(() => {
    const id = this.taskId();
    if (id) this.tasksService.ensureDetailLoaded(id);
    return id ? this.tasksService.byId(id)() : undefined;
  });

  /** Membres assignables : l'équipe DSI réelle rattachée au projet (seule éligible côté backend pour une assignation de tâche). */
  protected readonly agentsAffectables = computed(() => {
    const t = this.task();
    if (!t) return [];
    const equipeId = this.projectsService.byId(t.projectId)()?.equipeId;
    const equipe = equipeId ? this.equipesService.teams().find((e) => e.id === equipeId) : undefined;
    const source = equipe ? [equipe.chefId, ...equipe.membreIds] : [];
    return source
      .filter((id) => !t.assigneIds.includes(id))
      .map((id) => this.peopleService.byId(id))
      .filter((p) => p !== undefined);
  });

  protected readonly bloquePar = computed(() => {
    const t = this.task();
    if (!t) return [];
    return this.tasksService
      .dependancesBloquantes(t.id)()
      .map((dep) => ({ dep, tache: this.tasksService.byId(dep.idTacheSource)() }))
      .filter((x): x is { dep: (typeof x)['dep']; tache: Task } => x.tache !== undefined);
  });

  /** Tâche préalable non terminée qui empêche le démarrage de celle-ci — dérivée des vraies dépendances (chargées via ensureDetailLoaded). */
  protected readonly tacheBloquante = computed(() =>
    this.bloquePar().find((item) => item.tache.statut !== 'terminee' && item.tache.statut !== 'reportee')?.tache,
  );

  protected readonly bloque = computed(() => {
    const t = this.task();
    if (!t) return [];
    return this.tasksService
      .dependancesBloquees(t.id)()
      .map((dep) => ({ dep, tache: this.tasksService.byId(dep.idTacheCible)() }))
      .filter((x): x is { dep: (typeof x)['dep']; tache: Task } => x.tache !== undefined);
  });

  protected readonly totalDependances = computed(() => this.bloquePar().length + this.bloque().length);

  protected readonly assignes = computed(() =>
    (this.task()?.assigneIds ?? []).map((id) => this.peopleService.byId(id)).filter((p) => p !== undefined),
  );

  protected readonly sousTachesFaites = computed(() => this.task()?.sousTaches.filter((s) => s.fait).length ?? 0);

  /** Temps passé « en direct » — avance tout seul tant que le chronomètre de la tâche tourne. */
  protected readonly tempsPasseLive = computed(() => {
    const t = this.task();
    return t ? tempsPasseActuel(t, this.horlogeService.maintenant()) : 0;
  });

  personneNom(id: string | undefined): string {
    return id ? (this.peopleService.byId(id)?.nom ?? 'Système') : 'Système';
  }

  personne(id: string | undefined) {
    return id ? this.peopleService.byId(id) : undefined;
  }

  protected readonly formatDateFr = formatDateFr;
  protected readonly formatHours = formatHours;
  protected readonly formatHms = formatHms;

  async toggleSousTache(sousTacheId: string): Promise<void> {
    if (!this.peutGererTache()) return;
    const task = this.task();
    if (task) await this.tasksService.toggleSousTache(task.id, sousTacheId).catch(() => undefined);
  }

  async supprimerSousTache(sousTacheId: string): Promise<void> {
    if (!this.peutGererTache()) return;
    const task = this.task();
    if (task) await this.tasksService.removeSousTache(task.id, sousTacheId).catch(() => undefined);
  }

  async supprimerDependance(depId: string): Promise<void> {
    if (!this.peutGererTache()) return;
    await this.tasksService.supprimerDependance(depId).catch(() => undefined);
  }

  ouvrirLienModal(): void {
    this.modalLienOuvert.set(true);
  }

  fermerLienModal(): void {
    this.modalLienOuvert.set(false);
  }

  async ajouterSousTache(): Promise<void> {
    if (!this.peutGererTache()) return;
    const titre = this.nouvelleSousTache().trim();
    const task = this.task();
    if (!titre || !task) return;
    this.nouvelleSousTache.set('');
    await this.tasksService.addSousTache(task.id, titre).catch(() => undefined);
  }

  async affecterAgent(): Promise<void> {
    if (!this.peutGererTache()) return;
    const agentId = this.agentAAffecter();
    const task = this.task();
    if (!agentId || !task) return;
    this.agentAAffecter.set('');
    await this.tasksService.toggleAssignation(task.id, agentId).catch(() => undefined);
  }

  async retirerAssignation(personneId: string): Promise<void> {
    if (!this.peutGererTache()) return;
    const task = this.task();
    if (task) await this.tasksService.toggleAssignation(task.id, personneId).catch(() => undefined);
  }

  async envoyer(): Promise<void> {
    const texte = this.commentaire().trim();
    const fichier = this.fichierSelectionne();
    const task = this.task();
    const user = this.authService.currentUser();
    if (!task || !user) return;
    if (!texte && !fichier) return;

    this.commentaire.set('');
    this.fichierSelectionne.set(null);

    if (fichier) {
      await this.tasksService.addCommentWithAttachment(task.id, user.id, texte, fichier).catch(() => undefined);
    } else {
      await this.tasksService.addComment(task.id, user.id, texte).catch(() => undefined);
    }
  }

  selectionnerFichier(event: Event): void {
    const fichier = (event.target as HTMLInputElement).files?.[0];
    this.fichierSelectionne.set(fichier ?? null);
    (event.target as HTMLInputElement).value = '';
  }

  retirerFichierSelectionne(): void {
    this.fichierSelectionne.set(null);
  }

  async telechargerPieceJointe(activite: { id: string; pieceJointe?: { nomFichier: string } }): Promise<void> {
    const task = this.task();
    if (!task || !activite.pieceJointe) return;
    await this.tasksService.telechargerPieceJointe(task.id, activite.id, activite.pieceJointe.nomFichier).catch(() => undefined);
  }

  formatTaille(octets: number): string {
    if (octets < 1024) return `${octets} o`;
    if (octets < 1024 * 1024) return `${Math.round(octets / 1024)} Ko`;
    return `${(octets / (1024 * 1024)).toFixed(1)} Mo`;
  }
}
