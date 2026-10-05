import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PeopleService } from '@core/services/people.service';
import { RolesService } from '@core/services/roles.service';
import { Person } from '@core/models/person.model';
import { Role, ROLE_LABELS, ROLE_STYLE } from '@core/models/role.model';
import { Avatar } from '@shared/components/avatar/avatar';
import { SearchBar } from '@shared/components/search-bar/search-bar';
import { DropdownMenu, DropdownItem } from '@shared/components/dropdown-menu/dropdown-menu';
import { Modal } from '@shared/components/modal/modal';
import { ConfirmationDialog } from '@shared/components/confirmation-dialog/confirmation-dialog';
import { ToastService } from '@shared/components/toast/toast.service';

const CARACTERES_MOT_DE_PASSE = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789';

type FiltreStatut = 'tous' | 'actif' | 'suspendu';

@Component({
  selector: 'app-users-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Avatar, SearchBar, DropdownMenu, Modal, ConfirmationDialog],
  templateUrl: './users-page.html',
  styleUrl: './users-page.css',
})
export class UsersPage {
  private readonly fb = inject(FormBuilder);
  private readonly peopleService = inject(PeopleService);
  private readonly rolesService = inject(RolesService);
  private readonly toast = inject(ToastService);

  protected readonly roleLabels = ROLE_LABELS;
  protected readonly roleStyle = ROLE_STYLE;
  protected readonly search = signal('');

  /** Rôles proposés à la création/modification d'un compte : reflète la table `roles` en base, pas seulement les 5 rôles système connus du frontend. */
  protected readonly roleItems = computed<DropdownItem[]>(() =>
    this.rolesService.roles().map((r) => ({ value: r.nom, label: ROLE_LABELS[r.nom as Role] ?? r.nom })),
  );

  protected readonly roleFiltre = signal<'tous' | Role>('tous');
  protected readonly roleFiltreItems: DropdownItem[] = [{ value: 'tous', label: 'Tous les rôles' }, ...Object.values(Role).map((r) => ({ value: r, label: ROLE_LABELS[r] }))];
  protected readonly roleFiltreLabel = computed(() => (this.roleFiltre() === 'tous' ? 'Tous les rôles' : ROLE_LABELS[this.roleFiltre() as Role]));

  protected readonly statutFiltre = signal<FiltreStatut>('tous');
  protected readonly statutFiltreItems: DropdownItem[] = [
    { value: 'tous', label: 'Tous les comptes' },
    { value: 'actif', label: 'Actif' },
    { value: 'suspendu', label: 'Déconnecté' },
  ];
  protected readonly statutFiltreLabel = computed(() => this.statutFiltreItems.find((i) => i.value === this.statutFiltre())!.label);

  protected readonly people = this.peopleService.people;
  protected readonly activeCount = this.peopleService.activeCount;
  protected readonly suspendedCount = this.peopleService.suspendedCount;

  constructor() {
    // Recharge systématiquement depuis le backend à chaque ouverture de la page : les comptes
    // peuvent avoir été modifiés hors de l'application, le cache en mémoire de PeopleService ne
    // doit pas rester figé.
    this.peopleService.reload();
  }

  protected readonly filteredPeople = computed(() => {
    const term = this.search().trim().toLowerCase();
    const role = this.roleFiltre();
    const statut = this.statutFiltre();

    return this.people().filter((p) => {
      if (role !== 'tous' && p.role !== role) return false;
      if (statut !== 'tous' && p.statut !== statut) return false;
      if (term && !(p.nom.toLowerCase().includes(term) || p.email.toLowerCase().includes(term) || p.matricule.includes(term))) return false;
      return true;
    });
  });

  protected readonly modalOuvert = signal(false);
  protected readonly personneEnEdition = signal<Person | null>(null);
  protected readonly modeEdition = computed(() => this.personneEnEdition() !== null);
  protected readonly personneASupprimer = signal<Person | null>(null);
  /** Second palier de confirmation : le compte est encore assigné à des tâches, la suppression normale a été refusée. */
  protected readonly personneAvecTachesASupprimer = signal<Person | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    matricule: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: [''],
    role: this.fb.nonNullable.control<Role>(Role.MembreEquipe, Validators.required),
    specialite: ['', Validators.required],
    motDePasse: [''],
  });

  async toggleStatut(id: string, statut: 'actif' | 'suspendu'): Promise<void> {
    try {
      await this.peopleService.setStatus(id, statut === 'actif' ? 'suspendu' : 'actif');
      this.toast.show({ type: 'success', message: statut === 'actif' ? 'Compte suspendu.' : 'Compte réactivé.' });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  selectRole(item: DropdownItem): void {
    this.form.controls.role.setValue(item.value as Role);
  }

  selectRoleFiltre(item: DropdownItem): void {
    this.roleFiltre.set(item.value as 'tous' | Role);
  }

  selectStatutFiltre(item: DropdownItem): void {
    this.statutFiltre.set(item.value as FiltreStatut);
  }

  genererMotDePasse(): void {
    let motDePasse = '';
    for (let i = 0; i < 10; i++) {
      motDePasse += CARACTERES_MOT_DE_PASSE.charAt(Math.floor(Math.random() * CARACTERES_MOT_DE_PASSE.length));
    }
    this.form.controls.motDePasse.setValue(motDePasse);
  }

  ouvrirCreationCompte(): void {
    this.personneEnEdition.set(null);
    this.form.reset({ prenom: '', nom: '', matricule: '', email: '', telephone: '', role: Role.MembreEquipe, specialite: '', motDePasse: '' });
    this.form.controls.motDePasse.setValidators([Validators.required, Validators.minLength(6)]);
    this.form.controls.motDePasse.updateValueAndValidity();
    this.modalOuvert.set(true);
  }

  ouvrirModification(personne: Person): void {
    this.personneEnEdition.set(personne);
    this.form.reset({
      prenom: personne.prenom,
      nom: personne.nomUsuel,
      matricule: personne.matricule,
      email: personne.email,
      telephone: personne.telephone ?? '',
      role: personne.role,
      specialite: personne.direction,
      motDePasse: '',
    });
    this.form.controls.motDePasse.setValidators([Validators.minLength(6)]);
    this.form.controls.motDePasse.updateValueAndValidity();
    this.modalOuvert.set(true);
  }

  fermerCreationCompte(): void {
    this.modalOuvert.set(false);
  }

  async enregistrerCompte(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { prenom, nom, matricule, email, telephone, role, specialite, motDePasse } = this.form.getRawValue();
    const enEdition = this.personneEnEdition();
    const nomComplet = `${prenom} ${nom}`;

    try {
      if (enEdition) {
        await this.peopleService.update(enEdition.id, { prenom, nom, matricule, email, telephone, role, direction: specialite, motDePasse: motDePasse || undefined });
        this.toast.show({ type: 'success', message: `Compte de ${nomComplet} modifié.` });
      } else {
        await this.peopleService.create({ prenom, nom, matricule, email, telephone, role, direction: specialite, motDePasse });
        this.toast.show({ type: 'success', message: `Compte créé pour ${nomComplet} (${matricule}).` });
      }
      this.modalOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global (ex. matricule/courriel déjà utilisé).
    }
  }

  demanderSuppression(personne: Person): void {
    this.personneASupprimer.set(personne);
  }

  annulerSuppression(): void {
    this.personneASupprimer.set(null);
  }

  async confirmerSuppression(): Promise<void> {
    const personne = this.personneASupprimer();
    if (!personne) return;
    this.personneASupprimer.set(null);
    try {
      await this.peopleService.delete(personne.id);
      this.toast.show({ type: 'success', message: `Compte de ${personne.nom} supprimé.` });
    } catch (erreur) {
      if (erreur instanceof HttpErrorResponse && erreur.status === 409) {
        // Compte encore assigné à une ou plusieurs tâches : proposer de le retirer avant de le supprimer.
        this.personneAvecTachesASupprimer.set(personne);
        return;
      }
      // Autre erreur : message déjà affiché par l'intercepteur HTTP global.
    }
  }

  annulerSuppressionAvecTaches(): void {
    this.personneAvecTachesASupprimer.set(null);
  }

  async confirmerSuppressionAvecTaches(): Promise<void> {
    const personne = this.personneAvecTachesASupprimer();
    if (!personne) return;
    this.personneAvecTachesASupprimer.set(null);
    try {
      await this.peopleService.delete(personne.id, true);
      this.toast.show({ type: 'success', message: `Compte de ${personne.nom} supprimé et retiré de ses tâches.` });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }
}
