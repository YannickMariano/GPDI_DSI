import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PeopleService } from '@core/services/people.service';
import { HabilitationsService } from '@core/services/habilitations.service';
import { RolesService } from '@core/services/roles.service';
import { Role, ROLE_LABELS, ROLE_STYLE } from '@core/models/role.model';
import { HabilitationLigneRequest, NiveauAcces, PermissionRow } from '@core/models/habilitation.model';
import { Modal } from '@shared/components/modal/modal';
import { ToastService } from '@shared/components/toast/toast.service';

const ROLE_MISSIONS: Record<Role, string> = {
  [Role.DirecteurDsi]: 'Consulte le portefeuille, les indicateurs et le journal des connexions. Aucun droit de modification opérationnelle.',
  [Role.ChefProjet]: 'Crée les projets, planifie les sprints et les jalons, crée et affecte les tâches, partage l\'avancement au client.',
  [Role.MembreEquipe]: 'Fait évoluer le statut de ses tâches, enregistre son temps, consulte les dépendances.',
  [Role.Administrateur]: 'Administre les comptes, les rôles, les habilitations, les rattachements et les référentiels.',
  [Role.DirectionMetier]: 'Consulte uniquement l\'état d\'avancement des projets qui lui sont explicitement partagés.',
};

const CELL_MARK: Record<NiveauAcces, { marque: string; bg: string; color: string }> = {
  ecriture: { marque: '✓', bg: 'var(--color-success-bg)', color: 'var(--color-success-text)' },
  lecture: { marque: 'L', bg: 'var(--color-action-bg)', color: 'var(--color-action)' },
  aucun: { marque: '–', bg: 'var(--color-surface-muted)', color: 'var(--color-border-decorative)' },
};

/** Cycle de bascule au clic sur une case de la matrice : Lecture → Écriture → Aucun → Lecture… */
const CYCLE: NiveauAcces[] = ['lecture', 'ecriture', 'aucun'];

function clonerMatrice(matrice: PermissionRow[]): PermissionRow[] {
  return matrice.map((ligne) => ({ ...ligne, acces: { ...ligne.acces } }));
}

interface RoleCard {
  role: Role;
  nom: string;
  style: { color: string; bg: string };
  comptes: number;
  mission: string;
  droitsOuverts: number;
}

@Component({
  selector: 'app-roles-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Modal],
  templateUrl: './roles-page.html',
  styleUrl: './roles-page.css',
})
export class RolesPage {
  private readonly fb = inject(FormBuilder);
  private readonly peopleService = inject(PeopleService);
  private readonly habilitationsService = inject(HabilitationsService);
  private readonly rolesService = inject(RolesService);
  private readonly toast = inject(ToastService);

  protected readonly roleLabels = ROLE_LABELS;
  protected readonly roleStyle = ROLE_STYLE;
  protected readonly roles = Object.values(Role);
  protected readonly cellMark = CELL_MARK;
  protected readonly fonctionsDisponibles = computed(() => this.matrice().map((l) => l.fonction));

  protected readonly matrice = signal<PermissionRow[]>([]);
  protected readonly aDesModifications = signal(false);

  constructor() {
    // Resynchronise la copie locale editable avec la matrice serveur, sauf pendant une edition en cours
    // (cases deja basculees ou modale de role ouverte) pour ne pas ecraser un travail non enregistre.
    effect(() => {
      const matriceServeur = this.habilitationsService.matrice();
      if (!this.aDesModifications()) {
        this.matrice.set(clonerMatrice(matriceServeur));
      }
    });
  }

  protected readonly roleCards = computed<RoleCard[]>(() => {
    const matrice = this.matrice();
    return this.roles.map((role) => ({
      role,
      nom: ROLE_LABELS[role],
      style: ROLE_STYLE[role],
      comptes: this.peopleService.byRole(role).length,
      mission: this.rolesService.byNom(role)?.description || ROLE_MISSIONS[role],
      droitsOuverts: matrice.filter((row) => row.acces[role] !== 'aucun').length,
    }));
  });

  cyclerCellule(fonction: string, role: Role): void {
    this.matrice.update((matrice) =>
      matrice.map((ligne) => {
        if (ligne.fonction !== fonction) return ligne;
        const actuel = ligne.acces[role];
        const suivant = CYCLE[(CYCLE.indexOf(actuel) + 1) % CYCLE.length];
        return { ...ligne, acces: { ...ligne.acces, [role]: suivant } };
      }),
    );
    this.aDesModifications.set(true);
  }

  annulerModifications(): void {
    this.aDesModifications.set(false);
  }

  async enregistrer(): Promise<void> {
    const matriceServeur = this.habilitationsService.matrice();
    const lignesModifiees: HabilitationLigneRequest[] = [];
    for (const ligne of this.matrice()) {
      const ligneServeur = matriceServeur.find((l) => l.fonction === ligne.fonction);
      for (const role of this.roles) {
        if (!ligneServeur || ligneServeur.acces[role] !== ligne.acces[role]) {
          lignesModifiees.push({ fonction: ligne.fonction, roleNom: role, niveau: ligne.acces[role] });
        }
      }
    }
    if (lignesModifiees.length === 0) {
      this.aDesModifications.set(false);
      return;
    }
    try {
      await this.habilitationsService.mettreAJour(lignesModifiees);
      this.aDesModifications.set(false);
      this.toast.show({ type: 'success', message: 'Matrice des habilitations mise à jour.' });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  // --- Édition des permissions d'un rôle système ---

  protected readonly modalRoleOuvert = signal(false);
  protected readonly roleEnEdition = signal<RoleCard | null>(null);
  protected readonly permissionsRoleEnEdition = signal<Record<string, NiveauAcces>>({});

  protected readonly formRole = this.fb.nonNullable.group({
    nom: ['', Validators.required],
    description: [''],
  });

  /**
   * Le contenu projeté dans <app-modal> (ng-content) est toujours évalué par Angular même modale fermée :
   * un objet vide ferait planter les lectures cellMark[...] du template avant la première ouverture.
   */
  niveauEdition(fonction: string): NiveauAcces {
    return this.permissionsRoleEnEdition()[fonction] ?? 'aucun';
  }

  ouvrirModificationRole(carte: RoleCard): void {
    this.roleEnEdition.set(carte);
    this.formRole.reset({ nom: carte.nom, description: carte.mission });
    this.formRole.controls.nom.disable();

    const permissions: Record<string, NiveauAcces> = {};
    for (const ligne of this.matrice()) permissions[ligne.fonction] = ligne.acces[carte.role];
    this.permissionsRoleEnEdition.set(permissions);

    this.modalRoleOuvert.set(true);
  }

  fermerCreationRole(): void {
    this.modalRoleOuvert.set(false);
  }

  cyclerPermissionRoleEnEdition(fonction: string): void {
    this.permissionsRoleEnEdition.update((carte) => {
      const actuel = carte[fonction];
      const suivant = CYCLE[(CYCLE.indexOf(actuel) + 1) % CYCLE.length];
      return { ...carte, [fonction]: suivant };
    });
  }

  async enregistrerRole(): Promise<void> {
    const enEdition = this.roleEnEdition();
    if (!enEdition) return;

    const { description } = this.formRole.getRawValue();
    const enregistrement = this.rolesService.byNom(enEdition.role);
    if (!enregistrement) {
      this.toast.show({ type: 'error', message: `Rôle « ${enEdition.nom} » introuvable en base.` });
      return;
    }
    try {
      await this.rolesService.modifierDescription(enregistrement.id, enregistrement.nom, description);
    } catch {
      return; // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }

    const permissions = this.permissionsRoleEnEdition();
    const role = enEdition.role;
    this.matrice.update((matrice) => matrice.map((ligne) => ({ ...ligne, acces: { ...ligne.acces, [role]: permissions[ligne.fonction] } })));
    this.aDesModifications.set(true);
    this.toast.show({ type: 'success', message: `Rôle « ${enEdition.nom} » modifié.` });
    this.modalRoleOuvert.set(false);
  }
}
