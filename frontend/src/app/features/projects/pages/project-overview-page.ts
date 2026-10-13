import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TasksService } from '@core/services/tasks.service';
import { MilestonesService } from '@core/services/milestones.service';
import { SprintsService } from '@core/services/sprints.service';
import { BurndownService } from '@core/services/burndown.service';
import { PeopleService } from '@core/services/people.service';
import { AuthService } from '@core/services/auth.service';
import { PointsInformationService } from '@core/services/points-information.service';
import { Role } from '@core/models/role.model';
import { KpiCard } from '@shared/components/kpi-card/kpi-card';
import { ProgressBar } from '@shared/components/progress-bar/progress-bar';
import { Avatar } from '@shared/components/avatar/avatar';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { Modal } from '@shared/components/modal/modal';
import { ToastService } from '@shared/components/toast/toast.service';
import { RelativeTimePipe } from '@shared/pipes/relative-time.pipe';
import { formatDateFr } from '@shared/utils/format.util';
import { computeWorkload, etatCharge } from '../workload.util';
import { Milestone } from '@core/models/milestone.model';

@Component({
  selector: 'app-project-overview-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, KpiCard, ProgressBar, Avatar, EmptyState, Modal, ReactiveFormsModule, RelativeTimePipe],
  templateUrl: './project-overview-page.html',
  styleUrl: './project-overview-page.css',
})
export class ProjectOverviewPage {
  readonly projectId = input.required<string>();

  private readonly fb = inject(FormBuilder);
  private readonly tasksService = inject(TasksService);
  private readonly milestonesService = inject(MilestonesService);
  private readonly sprintsService = inject(SprintsService);
  private readonly burndownService = inject(BurndownService);
  private readonly peopleService = inject(PeopleService);
  private readonly authService = inject(AuthService);
  private readonly pointsInformationService = inject(PointsInformationService);
  private readonly toast = inject(ToastService);

  protected readonly etatCharge = etatCharge;
  protected readonly peutPublier = computed(() => this.authService.currentUser()?.role === Role.ChefProjet);
  protected readonly pointsInformation = computed(() => this.pointsInformationService.byProject(this.projectId())());

  protected readonly sprintEnCours = computed(() => this.sprintsService.current(this.projectId())());
  protected readonly burndown = computed(() => {
    const sprint = this.sprintEnCours();
    return sprint ? this.burndownService.byProjectAndSprint(this.projectId(), sprint.id)() : [];
  });
  protected readonly maxBurndown = computed(() => {
    const points = this.burndown();
    return points.length > 0 ? Math.max(1, ...points.map((p) => Math.max(p.restant, p.ideal))) : 1;
  });

  protected readonly tasks = computed(() => this.tasksService.byProject(this.projectId())());
  protected readonly tachesOuvertes = computed(() => this.tasks().filter((t) => t.statut !== 'terminee').length);
  /** Affichage borné à 100 % max : au-delà, on ne montre jamais le dépassement exact, seulement « + de 100 ». */
  protected readonly chargeConsommee = computed<number | string>(() => {
    const enCours = this.tasks().filter((t) => t.sprintId);
    const estime = enCours.reduce((s, t) => s + t.estimeHeures, 0);
    const passe = enCours.reduce((s, t) => s + t.tempsPasseHeures, 0);
    const pct = estime > 0 ? Math.round((passe / estime) * 100) : 0;
    return pct > 100 ? '+ de 100' : pct;
  });

  protected readonly milestones = computed(() => this.milestonesService.byProject(this.projectId())());
  protected readonly jalonsTenusPct = computed(() => {
    const jalons = this.milestones();
    if (jalons.length === 0) return 0;
    return Math.round((jalons.filter((j) => j.statut === 'atteint').length / jalons.length) * 100);
  });
  protected readonly prochainsJalons = computed(() => this.milestones().filter((j) => j.statut !== 'atteint').slice(0, 3));

  protected readonly workload = computed(() => computeWorkload(this.tasks(), this.peopleService.people()).slice(0, 5));

  protected readonly activiteRecente = computed(() => {
    const entries = this.tasks().flatMap((t) => t.activite.map((a) => ({ ...a, tacheCode: t.code })));
    return entries.sort((a, b) => new Date(b.quand).getTime() - new Date(a.quand).getTime()).slice(0, 5);
  });

  protected readonly formatDateFr = formatDateFr;

  auteurNom(id: string | undefined): string {
    return id ? (this.peopleService.byId(id)?.nom ?? 'Système') : 'Système';
  }

  auteurInitiales(id: string | undefined) {
    return id ? this.peopleService.byId(id) : undefined;
  }

  livrablesRestants(jalon: Milestone): number {
    return jalon.livrables.filter((l) => l.statut !== 'livre').length;
  }

  protected readonly modalPublicationOuvert = signal(false);

  protected readonly formPublication = this.fb.nonNullable.group({
    titre: ['', [Validators.required, Validators.maxLength(300)]],
    corps: ['', Validators.required],
  });

  ouvrirPublication(): void {
    this.formPublication.reset({ titre: '', corps: '' });
    this.modalPublicationOuvert.set(true);
  }

  fermerPublication(): void {
    this.modalPublicationOuvert.set(false);
  }

  async confirmerPublication(): Promise<void> {
    if (this.formPublication.invalid) {
      this.formPublication.markAllAsTouched();
      return;
    }
    try {
      await this.pointsInformationService.publier(this.projectId(), this.formPublication.getRawValue());
      this.toast.show({ type: 'success', message: 'Point d\'information publié auprès du client.' });
      this.modalPublicationOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }
}
