import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TasksService } from '@core/services/tasks.service';
import { PeopleService } from '@core/services/people.service';
import { AuthService } from '@core/services/auth.service';
import { ProjectsService } from '@core/services/projects.service';
import { EquipesService } from '@core/services/equipes.service';
import { Role } from '@core/models/role.model';
import { ToastService } from '@shared/components/toast/toast.service';
import { Avatar } from '@shared/components/avatar/avatar';
import { ProgressBar } from '@shared/components/progress-bar/progress-bar';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { Modal } from '@shared/components/modal/modal';
import { computeWorkload, etatCharge, MemberWorkload } from '../workload.util';

@Component({
  selector: 'app-team-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, ProgressBar, EmptyState, Modal, ReactiveFormsModule],
  templateUrl: './team-page.html',
  styleUrl: './team-page.css',
})
export class TeamPage {
  readonly projectId = input.required<string>();

  private readonly fb = inject(FormBuilder);
  private readonly tasksService = inject(TasksService);
  private readonly peopleService = inject(PeopleService);
  private readonly authService = inject(AuthService);
  private readonly projectsService = inject(ProjectsService);
  private readonly equipesService = inject(EquipesService);
  private readonly toast = inject(ToastService);

  protected readonly etatCharge = etatCharge;
  protected readonly peutGererEquipe = computed(() => this.authService.currentUser()?.role === Role.ChefProjet);

  protected readonly project = computed(() => this.projectsService.byId(this.projectId())());

  /** Équipe DSI organisationnelle rattachée au projet (`Projet.idEquipe`), le cas échéant. */
  protected readonly equipe = computed(() => {
    const equipeId = this.project()?.equipeId;
    return equipeId ? this.equipesService.teams().find((e) => e.id === equipeId) : undefined;
  });

  /** Chef de projet + chef d'équipe + membres de l'équipe rattachée — dédupliqués. */
  protected readonly membresEquipeIds = computed<string[]>(() => {
    const ids = new Set<string>();
    const projet = this.project();
    if (projet) ids.add(projet.responsableId);
    const equipe = this.equipe();
    if (equipe) {
      ids.add(equipe.chefId);
      for (const membreId of equipe.membreIds) ids.add(membreId);
    }
    return [...ids];
  });

  protected readonly workload = computed<MemberWorkload[]>(() => {
    const base = computeWorkload(this.tasksService.byProject(this.projectId())(), this.peopleService.people());
    const idsAvecCharge = new Set(base.map((w) => w.person.id));
    const sansCharge = this.membresEquipeIds()
      .filter((id) => !idsAvecCharge.has(id))
      .map((id) => this.peopleService.byId(id))
      .filter((p) => p !== undefined)
      .map((person) => ({ person, heuresAffectees: 0, tachesEnCours: 0, tachesBloquees: 0, chargePct: 0 }) satisfies MemberWorkload);
    return [...base, ...sansCharge];
  });

  protected readonly surchargesCount = computed(() => this.workload().filter((w) => w.chargePct >= 100).length);

  protected readonly agentsDisponibles = computed(() => {
    const dejaAffectes = new Set(this.workload().map((w) => w.person.id));
    return this.peopleService.people().filter((p) => !dejaAffectes.has(p.id) && (p.role === Role.MembreEquipe || p.role === Role.ChefProjet));
  });

  suggestionPour(chargePct: number): string | null {
    if (chargePct >= 100) return "L'assistant propose de réaffecter une partie de la charge à un membre disponible.";
    if (chargePct < 40) return 'Sous-utilisé(e) : peut absorber davantage de tâches ce sprint.';
    return null;
  }

  protected readonly modalOuvert = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    agentId: ['', Validators.required],
  });

  affecterAgent(): void {
    this.form.reset({ agentId: '' });
    this.modalOuvert.set(true);
  }

  fermerAffectation(): void {
    this.modalOuvert.set(false);
  }

  /** Affecte l'agent à l'équipe DSI organisationnelle du projet — visible aussi sur les autres projets qui partagent cette même équipe. */
  async confirmerAffectation(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const equipe = this.equipe();
    if (!equipe) {
      this.toast.show({ type: 'error', message: "Ce projet n'a pas encore d'équipe DSI rattachée : affectez-en une depuis la fiche projet." });
      return;
    }

    const { agentId } = this.form.getRawValue();
    const agent = this.peopleService.byId(agentId);
    try {
      await this.equipesService.updateTeam(equipe.id, {
        nom: equipe.nom,
        description: equipe.description ?? '',
        chefId: equipe.chefId,
        membreIds: [...new Set([...equipe.membreIds, agentId])],
      });
      this.toast.show({ type: 'success', message: `${agent?.nom ?? 'Agent'} affecté(e) à l'équipe.` });
      this.modalOuvert.set(false);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }
}
