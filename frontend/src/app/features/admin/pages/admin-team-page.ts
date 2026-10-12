import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PeopleService } from '@core/services/people.service';
import { ProjectsService } from '@core/services/projects.service';
import { EquipesService, Team } from '@core/services/equipes.service';
import { Role } from '@core/models/role.model';
import { Avatar } from '@shared/components/avatar/avatar';
import { Modal } from '@shared/components/modal/modal';
import { ConfirmationDialog } from '@shared/components/confirmation-dialog/confirmation-dialog';
import { ToastService } from '@shared/components/toast/toast.service';

@Component({
  selector: 'app-admin-team-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, Modal, ConfirmationDialog, ReactiveFormsModule],
  templateUrl: './admin-team-page.html',
  styleUrl: './admin-team-page.css',
})
export class AdminTeamPage {
  private readonly fb = inject(FormBuilder);
  private readonly peopleService = inject(PeopleService);
  private readonly projectsService = inject(ProjectsService);
  private readonly equipesService = inject(EquipesService);
  private readonly toast = inject(ToastService);

  protected readonly teams = this.equipesService.teams;

  constructor() {
    // Recharge systématiquement depuis le backend à chaque ouverture de la page : équipes, comptes
    // et projets peuvent avoir été modifiés hors de l'application, les caches en mémoire de ces
    // services ne doivent pas rester figés.
    this.equipesService.reload();
    this.peopleService.reload();
    this.projectsService.reload();
  }

  // Le "chef d'équipe" n'est pas un rôle applicatif à part : c'est un membre d'équipe désigné responsable de cette équipe (pas de login dédié).
  // Seuls les utilisateurs de rôle « membre_equipe » peuvent être chef d'équipe ou membre d'une équipe.
  private readonly rolesEligibles = computed(() => this.peopleService.people().filter((p) => p.role === Role.MembreEquipe));
  protected readonly chefsDisponibles = computed(() => this.rolesEligibles().filter((p) => p.statut === 'actif'));
  protected readonly membresDisponibles = computed(() => this.rolesEligibles());

  protected readonly equipesAffichees = computed(() => {
    const projets = this.projectsService.projects();

    return this.teams().map((team) => {
      const projetsActifs = projets.filter((p) => p.equipeId === team.id && p.statut === 'en_cours').length;

      return {
        team,
        chef: this.peopleService.byId(team.chefId),
        membres: team.membreIds.map((id) => this.peopleService.byId(id)).filter((p) => p !== undefined),
        projetsActifs,
        estActive: projetsActifs > 0,
      };
    });
  });

  protected readonly modalOuvert = signal(false);
  protected readonly equipeEnEdition = signal<Team | null>(null);
  protected readonly membresSelectionnes = signal<Set<string>>(new Set());
  protected readonly equipeASupprimer = signal<Team | null>(null);
  protected readonly membreRecherche = signal('');

  protected readonly membresChoisis = computed(() => {
    const selection = this.membresSelectionnes();
    return this.membresDisponibles().filter((m) => selection.has(m.id));
  });

  protected readonly membresFiltres = computed(() => {
    const term = this.membreRecherche().trim().toLowerCase();
    const liste = this.membresDisponibles();
    if (!term) return liste;
    return liste.filter((m) => m.nom.toLowerCase().includes(term));
  });

  protected readonly form = this.fb.nonNullable.group({
    nom: ['', Validators.required],
    chefId: ['', Validators.required],
    description: [''],
  });

  protected readonly modeEdition = computed(() => this.equipeEnEdition() !== null);

  personneNom(id: string): string {
    return this.peopleService.byId(id)?.nom ?? '—';
  }

  membreCoche(id: string): boolean {
    return this.membresSelectionnes().has(id);
  }

  toggleMembre(id: string): void {
    this.membresSelectionnes.update((set) => {
      const copie = new Set(set);
      if (copie.has(id)) copie.delete(id);
      else copie.add(id);
      return copie;
    });
  }

  ouvrirCreation(): void {
    this.equipeEnEdition.set(null);
    this.membresSelectionnes.set(new Set());
    this.membreRecherche.set('');
    this.form.reset({ nom: '', chefId: '', description: '' });
    this.modalOuvert.set(true);
  }

  ouvrirModification(team: Team): void {
    this.equipeEnEdition.set(team);
    this.membresSelectionnes.set(new Set(team.membreIds));
    this.membreRecherche.set('');
    this.form.reset({ nom: team.nom, chefId: team.chefId, description: team.description ?? '' });
    this.modalOuvert.set(true);
  }

  fermerModal(): void {
    this.modalOuvert.set(false);
  }

  async enregistrerEquipe(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nom, chefId, description } = this.form.getRawValue();
    const membreIds = [...this.membresSelectionnes()];
    const enEdition = this.equipeEnEdition();

    try {
      if (enEdition) {
        await this.equipesService.updateTeam(enEdition.id, { nom, chefId, description, membreIds });
        this.toast.show({ type: 'success', message: `${nom} modifiée.` });
      } else {
        await this.equipesService.createTeam({ nom, chefId, description, membreIds });
        this.toast.show({ type: 'success', message: `${nom} créée.` });
      }
      this.modalOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  demanderSuppression(team: Team): void {
    this.equipeASupprimer.set(team);
  }

  annulerSuppression(): void {
    this.equipeASupprimer.set(null);
  }

  async confirmerSuppression(): Promise<void> {
    const team = this.equipeASupprimer();
    if (!team) return;
    try {
      await this.equipesService.deleteTeam(team.id);
      this.toast.show({ type: 'success', message: `${team.nom} supprimée.` });
    } finally {
      this.equipeASupprimer.set(null);
    }
  }
}
