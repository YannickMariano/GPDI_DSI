import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProjectsService } from '@core/services/projects.service';
import { PeopleService } from '@core/services/people.service';
import { AuthService } from '@core/services/auth.service';
import { EquipesService } from '@core/services/equipes.service';
import { TasksService } from '@core/services/tasks.service';
import { Project, StatutProjet } from '@core/models/project.model';
import { Priorite } from '@core/models/task.model';
import { Role } from '@core/models/role.model';
import { ProjectCard } from '@shared/components/project-card/project-card';
import { PriorityBadge } from '@shared/components/priority-badge/priority-badge';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { Modal } from '@shared/components/modal/modal';
import { ConfirmationDialog } from '@shared/components/confirmation-dialog/confirmation-dialog';
import { ToastService } from '@shared/components/toast/toast.service';
import { SearchBar } from '@shared/components/search-bar/search-bar';

const STATUT_STYLE: Record<StatutProjet, { label: string; bg: string; text: string }> = {
  en_preparation: { label: 'En préparation', bg: 'var(--status-a-faire-bg)', text: 'var(--status-a-faire-text)' },
  en_cours: { label: 'En cours', bg: 'var(--status-en-cours-bg)', text: 'var(--status-en-cours-text)' },
  suspendu: { label: 'Suspendu', bg: 'var(--priority-haute-bg)', text: 'var(--priority-haute-text)' },
  cloture: { label: 'Clôturé', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)' },
  archive: { label: 'Archivé', bg: 'var(--color-surface-alt)', text: 'var(--color-text-tertiary)' },
};

const PRIORITE_LABEL: Record<Priorite, string> = {
  critique: 'Critique',
  haute: 'Haute',
  normale: 'Normale',
  basse: 'Basse',
};

@Component({
  selector: 'app-my-projects-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ProjectCard, PriorityBadge, EmptyState, Modal, ConfirmationDialog, ReactiveFormsModule, SearchBar],
  templateUrl: './my-projects-page.html',
  styleUrl: './my-projects-page.css',
})
export class MyProjectsPage {
  private readonly fb = inject(FormBuilder);
  private readonly projectsService = inject(ProjectsService);
  private readonly peopleService = inject(PeopleService);
  private readonly authService = inject(AuthService);
  private readonly equipesService = inject(EquipesService);
  private readonly tasksService = inject(TasksService);
  private readonly toast = inject(ToastService);

  protected readonly statutStyle = STATUT_STYLE;
  protected readonly prioriteLabel = PRIORITE_LABEL;

  protected readonly projets = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return [];
    return this.projectsService.pourUtilisateur(user, this.tasksService.tasks());
  });

  protected readonly peutGererProjets = computed(() => this.authService.currentUser()?.role === Role.ChefProjet);

  protected readonly chefsDisponibles = computed(() => this.peopleService.byRole(Role.ChefProjet));
  protected readonly equipesDisponibles = this.equipesService.teams;

  constructor() {
    // Recharge systématiquement depuis le backend à chaque ouverture de la page : les projets,
    // comptes et équipes peuvent avoir été modifiés hors de l'application, les caches en mémoire de
    // ces services ne doivent pas rester figés.
    this.projectsService.reload();
    this.peopleService.reload();
    this.equipesService.reload();
  }

  // --- Recherche et filtres ---

  protected readonly recherche = signal('');
  protected readonly prioriteFiltre = signal<'toutes' | Priorite>('toutes');
  protected readonly statutFiltre = signal<'tous' | StatutProjet>('tous');

  protected readonly prioritesDisponibles: Priorite[] = ['critique', 'haute', 'normale', 'basse'];
  protected readonly statutsDisponibles: StatutProjet[] = ['en_preparation', 'en_cours', 'suspendu', 'cloture', 'archive'];

  protected readonly projetsFiltres = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    const priorite = this.prioriteFiltre();
    const statut = this.statutFiltre();

    return this.projets().filter((p) => {
      if (terme && !p.nom.toLowerCase().includes(terme)) return false;
      if (priorite !== 'toutes' && p.priorite !== priorite) return false;
      if (statut !== 'tous' && p.statut !== statut) return false;
      return true;
    });
  });

  responsable(id: string) {
    return this.peopleService.byId(id);
  }

  estArchive(projet: Project): boolean {
    return projet.statut === 'archive';
  }

  peutSuspendre(projet: Project): boolean {
    return projet.statut === 'en_cours' || projet.statut === 'en_preparation';
  }

  peutCloturer(projet: Project): boolean {
    return projet.statut === 'en_cours' || projet.statut === 'suspendu';
  }

  // --- Création / modification ---

  protected readonly modalOuvert = signal(false);
  protected readonly projetEnEdition = signal<Project | null>(null);
  protected readonly modeEdition = computed(() => this.projetEnEdition() !== null);

  protected readonly form = this.fb.nonNullable.group({
    nom: ['', Validators.required],
    description: ['', Validators.required],
    responsableId: ['', Validators.required],
    priorite: this.fb.nonNullable.control<Priorite>('normale', Validators.required),
    dateDebut: ['', Validators.required],
    dateFin: ['', Validators.required],
    equipeId: [''],
  });

  ouvrirCreation(): void {
    this.projetEnEdition.set(null);
    this.form.reset({ nom: '', description: '', responsableId: this.authService.currentUser()?.id ?? '', priorite: 'normale', dateDebut: '', dateFin: '', equipeId: '' });
    this.modalOuvert.set(true);
  }

  ouvrirModification(projet: Project): void {
    this.projetEnEdition.set(projet);
    this.form.reset({
      nom: projet.nom,
      description: projet.description,
      responsableId: projet.responsableId,
      priorite: projet.priorite,
      dateDebut: projet.dateDebut,
      dateFin: projet.dateFin,
      equipeId: projet.equipeId ?? '',
    });
    this.modalOuvert.set(true);
  }

  fermerModal(): void {
    this.modalOuvert.set(false);
  }

  async enregistrerProjet(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nom, description, responsableId, priorite, dateDebut, dateFin, equipeId } = this.form.getRawValue();
    if (dateFin < dateDebut) {
      this.toast.show({ type: 'error', message: 'La date de fin doit être postérieure à la date de début.' });
      return;
    }

    const direction = this.peopleService.byId(responsableId)?.direction ?? '';
    const enEdition = this.projetEnEdition();
    try {
      if (enEdition) {
        await this.projectsService.update(enEdition.id, { nom, description, direction, responsableId, priorite, dateDebut, dateFin, equipeId: equipeId || undefined });
        this.toast.show({ type: 'success', message: `${nom} modifié.` });
      } else {
        const projet = await this.projectsService.create({ nom, description, direction, responsableId, priorite, dateDebut, dateFin, equipeId: equipeId || undefined });
        this.toast.show({ type: 'success', message: `${nom} créé (${projet.codeReference}).` });
      }
      this.modalOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  // --- Archivage ---

  protected readonly projetAArchiver = signal<Project | null>(null);

  demanderArchivage(projet: Project): void {
    this.projetAArchiver.set(projet);
  }

  annulerArchivage(): void {
    this.projetAArchiver.set(null);
  }

  async confirmerArchivage(): Promise<void> {
    const projet = this.projetAArchiver();
    if (!projet) return;
    try {
      await this.projectsService.archiver(projet.id);
      this.toast.show({ type: 'success', message: `${projet.nom} archivé.` });
    } finally {
      this.projetAArchiver.set(null);
    }
  }

  // --- Suspension ---

  protected readonly projetASuspendre = signal<Project | null>(null);

  demanderSuspension(projet: Project): void {
    this.projetASuspendre.set(projet);
  }

  annulerSuspension(): void {
    this.projetASuspendre.set(null);
  }

  async confirmerSuspension(): Promise<void> {
    const projet = this.projetASuspendre();
    if (!projet) return;
    try {
      await this.projectsService.suspendre(projet.id);
      this.toast.show({ type: 'success', message: `${projet.nom} suspendu.` });
    } finally {
      this.projetASuspendre.set(null);
    }
  }

  // --- Clôture ---

  protected readonly projetACloturer = signal<Project | null>(null);

  demanderCloture(projet: Project): void {
    this.projetACloturer.set(projet);
  }

  annulerCloture(): void {
    this.projetACloturer.set(null);
  }

  async confirmerCloture(): Promise<void> {
    const projet = this.projetACloturer();
    if (!projet) return;
    try {
      await this.projectsService.cloturer(projet.id);
      this.toast.show({ type: 'success', message: `${projet.nom} clôturé.` });
    } finally {
      this.projetACloturer.set(null);
    }
  }
}
