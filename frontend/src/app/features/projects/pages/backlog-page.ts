import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TasksService } from '@core/services/tasks.service';
import { SprintsService } from '@core/services/sprints.service';
import { ProjectsService } from '@core/services/projects.service';
import { AuthService } from '@core/services/auth.service';
import { Role } from '@core/models/role.model';
import { Sprint } from '@core/models/sprint.model';
import { PriorityBadge } from '@shared/components/priority-badge/priority-badge';
import { StatusBadge } from '@shared/components/status-badge/status-badge';
import { ProgressBar } from '@shared/components/progress-bar/progress-bar';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { AiSuggestionChip } from '@shared/components/ai-suggestion-chip/ai-suggestion-chip';
import { Modal } from '@shared/components/modal/modal';
import { ConfirmationDialog } from '@shared/components/confirmation-dialog/confirmation-dialog';
import { ToastService } from '@shared/components/toast/toast.service';
import { formatDateFr, formatHours } from '@shared/utils/format.util';

const CAPACITE_PAR_DEFAUT_H = 40;

@Component({
  selector: 'app-backlog-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PriorityBadge, StatusBadge, ProgressBar, EmptyState, AiSuggestionChip, Modal, ConfirmationDialog, ReactiveFormsModule],
  templateUrl: './backlog-page.html',
  styleUrl: './backlog-page.css',
})
export class BacklogPage {
  readonly projectId = input.required<string>();

  private readonly fb = inject(FormBuilder);
  private readonly tasksService = inject(TasksService);
  private readonly sprintsService = inject(SprintsService);
  private readonly projectsService = inject(ProjectsService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly draggedTaskId = signal<string | null>(null);
  protected readonly formatDateFr = formatDateFr;
  protected readonly formatHours = formatHours;
  protected readonly peutGererSprints = computed(() => this.authService.currentUser()?.role === Role.ChefProjet);

  protected readonly backlog = computed(() => this.tasksService.backlog(this.projectId())());
  protected readonly backlogHeures = computed(() => this.backlog().reduce((s, t) => s + t.estimeHeures, 0));

  protected readonly sprintsProjet = computed(() => this.sprintsService.byProject(this.projectId())());

  /**
   * Tous les sprints du projet sont affichés ici, y compris ceux déjà clôturés (en lecture seule).
   * Toujours triés par ordre croissant (Sprint 1, Sprint 2, ...) via leur date de début — les sprints
   * se suivent chronologiquement sans jamais se chevaucher, donc cet ordre correspond à leur numéro.
   * Seuls les sprints CLÔTURÉS sont repoussés en fin de liste ; un sprint démarré (en cours) garde sa
   * place normale tant qu'il n'est pas clôturé.
   */
  protected readonly sprintsAffiches = computed(() => {
    const parDateDebut = [...this.sprintsProjet()].sort((a, b) => a.dateDebut.localeCompare(b.dateDebut));
    const actifs = parDateDebut.filter((s) => s.statut !== 'cloture');
    const clotures = parDateDebut.filter((s) => s.statut === 'cloture');
    return [...actifs, ...clotures];
  });

  constructor() {
    // Recharge systématiquement depuis le backend à chaque ouverture de la page (ou changement de
    // projet) : les sprints peuvent avoir été modifiés hors de l'application, le cache en mémoire
    // de SprintsService ne doit pas rester figé.
    effect(() => {
      this.sprintsService.reload(this.projectId());
    });
  }

  protected readonly modalOuvert = signal(false);
  protected readonly sprintEnEdition = signal<Sprint | null>(null);
  protected readonly modeEditionSprint = computed(() => this.sprintEnEdition() !== null);

  /**
   * Les sprints d'un projet se suivent toujours chronologiquement (jamais de chevauchement) : à la
   * création, la date de début ne peut donc pas être antérieure au lendemain de la fin du dernier
   * sprint existant. Sert à griser les dates indisponibles dans le sélecteur natif (attribut `min`).
   */
  protected readonly dateMinDebutCreation = computed<string | null>(() => {
    const sprints = this.sprintsProjet();
    if (sprints.length === 0) return null;
    const derniereFin = sprints.reduce((max, s) => (s.dateFin > max ? s.dateFin : max), sprints[0].dateFin);
    const lendemain = new Date(`${derniereFin}T00:00:00`);
    lendemain.setDate(lendemain.getDate() + 1);
    return lendemain.toISOString().slice(0, 10);
  });

  protected readonly form = this.fb.nonNullable.group({
    nom: ['', Validators.required],
    dateDebut: ['', Validators.required],
    dateFin: ['', Validators.required],
    objectif: ['', Validators.required],
    capaciteHeures: this.fb.nonNullable.control(CAPACITE_PAR_DEFAUT_H, [Validators.required, Validators.min(1)]),
  });

  // --- Défilement des cartes sprint ---

  protected readonly sprintsEtendus = signal<Set<string>>(new Set());

  estEtendu(sprintId: string): boolean {
    return this.sprintsEtendus().has(sprintId);
  }

  toggleExpansion(sprintId: string): void {
    this.sprintsEtendus.update((set) => {
      const copie = new Set(set);
      if (copie.has(sprintId)) copie.delete(sprintId);
      else copie.add(sprintId);
      return copie;
    });
  }

  tachesSprint(sprintId: string) {
    return this.tasksService.bySprint(sprintId)();
  }

  tachesTermineesSprint(sprintId: string): number {
    return this.tachesSprint(sprintId).filter((t) => t.statut === 'terminee').length;
  }

  onDragStart(taskId: string): void {
    this.draggedTaskId.set(taskId);
  }

  async onDrop(sprintId: string | undefined): Promise<void> {
    const taskId = this.draggedTaskId();
    this.draggedTaskId.set(null);
    if (!taskId) return;

    if (sprintId) {
      const sprint = this.sprintsProjet().find((s) => s.id === sprintId);
      if (sprint?.statut === 'cloture') {
        this.toast.show({ type: 'error', message: 'Ce sprint est clôturé : il ne peut plus recevoir de tâches.' });
        return;
      }
    }

    try {
      await this.tasksService.moveToSprint(taskId, sprintId);
      this.toast.show({ type: 'success', message: 'Tâche replanifiée.' });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  suggererRepartition(): void {
    this.toast.show({ type: 'info', message: "L'assistant IA analyse la capacité des sprints…" });
  }

  async retirerDuSprint(taskId: string, code: string): Promise<void> {
    try {
      await this.tasksService.retirerDuSprint(taskId);
      this.toast.show({ type: 'success', message: `${code} remise dans le backlog.` });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  // --- Création / modification du sprint ---

  ouvrirCreationSprint(): void {
    this.sprintEnEdition.set(null);
    const prochainNumero = this.sprintsProjet().length + 1;
    this.form.reset({ nom: `Sprint ${prochainNumero}`, dateDebut: '', dateFin: '', objectif: '', capaciteHeures: CAPACITE_PAR_DEFAUT_H });
    this.modalOuvert.set(true);
  }

  ouvrirModificationSprint(sprint: Sprint): void {
    this.sprintEnEdition.set(sprint);
    this.form.reset({ nom: sprint.nom, dateDebut: sprint.dateDebut, dateFin: sprint.dateFin, objectif: sprint.objectif, capaciteHeures: sprint.capaciteHeures });
    this.modalOuvert.set(true);
  }

  fermerCreationSprint(): void {
    this.modalOuvert.set(false);
  }

  async enregistrerSprint(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nom, dateDebut, dateFin, objectif, capaciteHeures } = this.form.getRawValue();
    if (dateFin < dateDebut) {
      this.toast.show({ type: 'error', message: 'La date de fin doit être postérieure à la date de début.' });
      return;
    }
    const dateFinProjet = this.projectsService.byId(this.projectId())()?.dateFin;
    if (dateFinProjet && (dateDebut > dateFinProjet || dateFin > dateFinProjet)) {
      this.toast.show({ type: 'error', message: `La date de début ou de fin du sprint ne peut pas dépasser le ${formatDateFr(dateFinProjet)}, date de fin du projet.` });
      return;
    }

    const enEdition = this.sprintEnEdition();
    try {
      if (enEdition) {
        await this.sprintsService.update(enEdition.id, { nom, dateDebut, dateFin, objectif, capaciteHeures });
        this.toast.show({ type: 'success', message: `${nom} modifié.` });
      } else {
        const sprint = await this.sprintsService.create({ projectId: this.projectId(), nom, dateDebut, dateFin, objectif, capaciteHeures });
        this.toast.show({ type: 'success', message: `${sprint.nom} créé.` });
      }
      this.modalOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  // --- Démarrage du sprint ---

  /**
   * Un sprint ne peut démarrer que si aucun autre sprint du projet n'est déjà en cours (le sprint en cours doit d'abord être clôturé),
   * et — démarrage manuel possible avant sa date de début — à condition que le sprint qui le précède soit clôturé.
   */
  peutDemarrer(sprint: Sprint): boolean {
    const precedent = this.sprintPrecedent(sprint);
    return (
      sprint.statut === 'planifie' &&
      !this.sprintsProjet().some((s) => s.statut === 'en_cours') &&
      (precedent === undefined || precedent.statut === 'cloture')
    );
  }

  /** Sprint qui précède immédiatement celui-ci (les sprints se suivent sans chevauchement, triés par date de début). */
  private sprintPrecedent(sprint: Sprint): Sprint | undefined {
    return [...this.sprintsProjet()]
      .filter((s) => s.dateDebut < sprint.dateDebut)
      .sort((a, b) => b.dateDebut.localeCompare(a.dateDebut))[0];
  }

  /** Sprint en cours dont la date de fin est dépassée : signalé « En retard », il reste clôturable (mêmes règles de clôture). */
  estEnRetard(sprint: Sprint): boolean {
    return sprint.statut === 'en_cours' && sprint.dateFin < dateDuJour();
  }

  async demarrerSprint(sprint: Sprint): Promise<void> {
    try {
      await this.sprintsService.demarrer(sprint.id);
      this.toast.show({ type: 'success', message: `${sprint.nom} démarré.` });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  // --- Clôture du sprint ---

  /** Un sprint en cours ne peut être clôturé que si toutes ses tâches sont terminées ou reportées. */
  peutCloturer(sprint: Sprint): boolean {
    return sprint.statut === 'en_cours' && this.tachesSprint(sprint.id).every((t) => t.statut === 'terminee' || t.statut === 'reportee');
  }

  protected readonly sprintACloturer = signal<Sprint | null>(null);

  demanderClotureSprint(sprint: Sprint): void {
    this.sprintACloturer.set(sprint);
  }

  annulerClotureSprint(): void {
    this.sprintACloturer.set(null);
  }

  async confirmerClotureSprint(): Promise<void> {
    const sprint = this.sprintACloturer();
    if (!sprint) return;
    try {
      await this.sprintsService.cloturer(sprint.id);
      this.toast.show({ type: 'success', message: `${sprint.nom} clôturé.` });
      // Le backend démarre aussitôt le sprint suivant si sa date de début est déjà arrivée : on recharge pour l'afficher.
      this.sprintsService.reload(this.projectId());
    } finally {
      this.sprintACloturer.set(null);
    }
  }

  // --- Report d'une tâche vers un autre sprint ---

  protected readonly tacheAReporter = signal<{ taskId: string; code: string; sprintId: string } | null>(null);
  protected readonly sprintCibleReport = signal('');

  /** Sprints pouvant recevoir la tâche : ni clôturés, ni le sprint où elle se trouve déjà. */
  protected readonly sprintsCiblesReport = computed(() => {
    const origine = this.tacheAReporter()?.sprintId;
    return this.sprintsAffiches().filter((s) => s.statut !== 'cloture' && s.id !== origine);
  });

  ouvrirReport(taskId: string, code: string, sprintId: string): void {
    this.tacheAReporter.set({ taskId, code, sprintId });
    this.sprintCibleReport.set('');
  }

  fermerReport(): void {
    this.tacheAReporter.set(null);
  }

  async confirmerReport(): Promise<void> {
    const report = this.tacheAReporter();
    const cible = this.sprintsCiblesReport().find((s) => s.id === this.sprintCibleReport());
    if (!report || !cible) return;
    try {
      await this.tasksService.moveToSprint(report.taskId, cible.id);
      this.toast.show({ type: 'success', message: `${report.code} reportée dans ${cible.nom}.` });
      this.tacheAReporter.set(null);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }
}

/** Date du jour au format AAAA-MM-JJ (heure locale), comparable aux dates des sprints. */
function dateDuJour(): string {
  const maintenant = new Date();
  const mois = String(maintenant.getMonth() + 1).padStart(2, '0');
  const jour = String(maintenant.getDate()).padStart(2, '0');
  return `${maintenant.getFullYear()}-${mois}-${jour}`;
}
