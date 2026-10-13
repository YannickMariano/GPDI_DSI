import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MilestonesService } from '@core/services/milestones.service';
import { ProjectsService } from '@core/services/projects.service';
import { AuthService } from '@core/services/auth.service';
import { Role } from '@core/models/role.model';
import { Livrable, Milestone } from '@core/models/milestone.model';
import { formatDateFr } from '@shared/utils/format.util';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { Modal } from '@shared/components/modal/modal';
import { ConfirmationDialog } from '@shared/components/confirmation-dialog/confirmation-dialog';
import { ToastService } from '@shared/components/toast/toast.service';

const STATUT_STYLE: Record<string, { label: string; bg: string; text: string; icone: string }> = {
  atteint: { label: 'Atteint', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)', icone: '✓' },
  a_risque: { label: 'À risque', bg: 'var(--status-bloquee-bg)', text: 'var(--status-bloquee-text)', icone: '!' },
  planifie: { label: 'Planifié', bg: 'var(--status-a-faire-bg)', text: 'var(--status-a-faire-text)', icone: '◇' },
  annule: { label: 'Annulé', bg: 'var(--color-surface-alt)', text: 'var(--color-text-tertiary)', icone: '✕' },
};

const LIVRABLE_STATUT_STYLE: Record<string, { label: string; bg: string; text: string }> = {
  a_livrer: { label: 'À livrer', bg: 'var(--status-a-faire-bg)', text: 'var(--status-a-faire-text)' },
  livre: { label: 'Livré', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)' },
  rejete: { label: 'Rejeté', bg: 'var(--status-bloquee-bg)', text: 'var(--status-bloquee-text)' },
};

const TYPES_LIVRABLE = ['Document', 'Session', 'Livrable technique', 'Infrastructure', 'Backlog', 'Autre'];

@Component({
  selector: 'app-milestones-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState, Modal, ConfirmationDialog, ReactiveFormsModule],
  templateUrl: './milestones-page.html',
  styleUrl: './milestones-page.css',
})
export class MilestonesPage {
  readonly projectId = input.required<string>();

  private readonly fb = inject(FormBuilder);
  private readonly milestonesService = inject(MilestonesService);
  private readonly projectsService = inject(ProjectsService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly statutStyle = STATUT_STYLE;
  protected readonly livrableStatutStyle = LIVRABLE_STATUT_STYLE;
  protected readonly typesLivrable = TYPES_LIVRABLE;
  protected readonly formatDateFr = formatDateFr;
  protected readonly peutGererJalons = computed(() => this.authService.currentUser()?.role === Role.ChefProjet);

  protected readonly milestones = computed(() => this.milestonesService.byProject(this.projectId())());
  protected readonly livrablesTotal = computed(() => this.milestones().reduce((s, m) => s + m.livrables.length, 0));
  protected readonly enRetardCount = computed(() => this.milestones().filter((m) => m.statut === 'a_risque').length);

  constructor() {
    // Recharge systématiquement depuis le backend à chaque ouverture de la page (ou changement de
    // projet) : les jalons peuvent avoir été modifiés hors de l'application (base de données, autre
    // utilisateur), et le cache en mémoire de MilestonesService ne doit pas rester figé.
    effect(() => {
      this.milestonesService.reload(this.projectId());
    });
  }

  // --- Création / modification du jalon ---

  protected readonly modalOuvert = signal(false);
  protected readonly jalonEnEdition = signal<Milestone | null>(null);
  protected readonly modeEditionJalon = computed(() => this.jalonEnEdition() !== null);

  protected readonly form = this.fb.nonNullable.group({
    nom: ['', Validators.required],
    date: ['', Validators.required],
    description: ['', Validators.required],
  });

  declarerJalon(): void {
    this.jalonEnEdition.set(null);
    this.form.reset({ nom: '', date: '', description: '' });
    this.modalOuvert.set(true);
  }

  ouvrirModificationJalon(jalon: Milestone): void {
    this.jalonEnEdition.set(jalon);
    this.form.reset({ nom: jalon.nom, date: jalon.date, description: jalon.description });
    this.modalOuvert.set(true);
  }

  fermerDeclaration(): void {
    this.modalOuvert.set(false);
  }

  async confirmerDeclaration(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nom, date, description } = this.form.getRawValue();
    const enEdition = this.jalonEnEdition();

    const dateFinProjet = this.projectsService.byId(this.projectId())()?.dateFin;
    if (dateFinProjet && date > dateFinProjet) {
      this.toast.show({ type: 'error', message: `La date du jalon ne peut pas dépasser le ${formatDateFr(dateFinProjet)}, date de fin du projet.` });
      return;
    }

    try {
      if (enEdition) {
        await this.milestonesService.update(enEdition.id, { nom, date, description });
        this.toast.show({ type: 'success', message: `Jalon « ${nom} » modifié.` });
      } else {
        await this.milestonesService.create({ projectId: this.projectId(), nom, date, description });
        this.toast.show({ type: 'success', message: `Jalon « ${nom} » déclaré.` });
      }
      this.modalOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  // --- Terminer / annuler le jalon ---

  protected readonly jalonATerminer = signal<Milestone | null>(null);

  demanderTerminaison(jalon: Milestone): void {
    this.jalonATerminer.set(jalon);
  }

  annulerTerminaison(): void {
    this.jalonATerminer.set(null);
  }

  async confirmerTerminaison(): Promise<void> {
    const jalon = this.jalonATerminer();
    if (!jalon) return;
    try {
      await this.milestonesService.terminer(jalon.id);
      this.toast.show({ type: 'success', message: `Jalon « ${jalon.nom} » terminé.` });
    } finally {
      this.jalonATerminer.set(null);
    }
  }

  protected readonly jalonAAnnuler = signal<Milestone | null>(null);

  demanderAnnulation(jalon: Milestone): void {
    this.jalonAAnnuler.set(jalon);
  }

  annulerAnnulation(): void {
    this.jalonAAnnuler.set(null);
  }

  async confirmerAnnulation(): Promise<void> {
    const jalon = this.jalonAAnnuler();
    if (!jalon) return;
    try {
      await this.milestonesService.annuler(jalon.id);
      this.toast.show({ type: 'success', message: `Jalon « ${jalon.nom} » annulé.` });
    } finally {
      this.jalonAAnnuler.set(null);
    }
  }

  peutClore(jalon: Milestone): boolean {
    return jalon.statut === 'planifie' || jalon.statut === 'a_risque';
  }

  /** Un jalon terminé (atteint) est verrouillé : ni modification, ni nouveau livrable. */
  peutModifier(jalon: Milestone): boolean {
    return jalon.statut !== 'atteint';
  }

  // --- Ajout d'un livrable ---

  protected readonly livrableModalOuvert = signal(false);
  protected readonly jalonPourLivrable = signal<Milestone | null>(null);

  protected readonly livrableForm = this.fb.nonNullable.group({
    description: ['', Validators.required],
    type: this.fb.nonNullable.control('Document', Validators.required),
    typeAutre: [''],
    dateLivraisonPrevue: ['', Validators.required],
  });

  ouvrirAjoutLivrable(jalon: Milestone): void {
    this.jalonPourLivrable.set(jalon);
    this.livrableForm.reset({ description: '', type: 'Document', typeAutre: '', dateLivraisonPrevue: '' });
    this.livrableModalOuvert.set(true);
  }

  fermerAjoutLivrable(): void {
    this.livrableModalOuvert.set(false);
  }

  async confirmerAjoutLivrable(): Promise<void> {
    if (this.livrableForm.invalid) {
      this.livrableForm.markAllAsTouched();
      return;
    }
    const jalon = this.jalonPourLivrable();
    if (!jalon) return;

    const { description, type, typeAutre, dateLivraisonPrevue } = this.livrableForm.getRawValue();
    if (dateLivraisonPrevue > jalon.date) {
      this.toast.show({ type: 'error', message: `La date de livraison ne peut pas dépasser le ${formatDateFr(jalon.date)}, date du jalon.` });
      return;
    }

    const typeFinal = type === 'Autre' ? typeAutre.trim() || 'Autre' : type;
    try {
      await this.milestonesService.addLivrable(jalon.id, { description, type: typeFinal, dateLivraisonPrevue });
      this.toast.show({ type: 'success', message: 'Livrable ajouté.' });
      this.livrableModalOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  // --- Livraison / rejet d'un livrable ---

  async livrerLivrable(jalon: Milestone, livrable: Livrable): Promise<void> {
    try {
      await this.milestonesService.livrerLivrable(jalon.id, livrable.id);
      this.toast.show({ type: 'success', message: `« ${livrable.description} » livré.` });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  protected readonly rejetModalOuvert = signal(false);
  protected readonly livrableARejeter = signal<{ jalon: Milestone; livrable: Livrable } | null>(null);

  protected readonly rejetForm = this.fb.nonNullable.group({
    motif: ['', Validators.required],
  });

  ouvrirRejet(jalon: Milestone, livrable: Livrable): void {
    this.livrableARejeter.set({ jalon, livrable });
    this.rejetForm.reset({ motif: '' });
    this.rejetModalOuvert.set(true);
  }

  fermerRejet(): void {
    this.rejetModalOuvert.set(false);
  }

  async confirmerRejet(): Promise<void> {
    if (this.rejetForm.invalid) {
      this.rejetForm.markAllAsTouched();
      return;
    }
    const cible = this.livrableARejeter();
    if (!cible) return;
    const { motif } = this.rejetForm.getRawValue();
    try {
      await this.milestonesService.rejeterLivrable(cible.jalon.id, cible.livrable.id, motif);
      this.toast.show({ type: 'success', message: `« ${cible.livrable.description} » rejeté.` });
      this.rejetModalOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }
}
