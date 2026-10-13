import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { ProjectsService } from '@core/services/projects.service';
import { PeopleService } from '@core/services/people.service';
import { SprintsService } from '@core/services/sprints.service';
import { TasksService } from '@core/services/tasks.service';
import { AuthService } from '@core/services/auth.service';
import { Role } from '@core/models/role.model';
import { SANTE_CONFIG } from '@shared/utils/sante-config';
import { formatSprintLabel, sprintAAfficher } from '@shared/utils/sprint-label.util';
import { formatDateFr } from '@shared/utils/format.util';
import { ProgressBar } from '@shared/components/progress-bar/progress-bar';
import { Tabs, TabItem } from '@shared/components/tabs/tabs';

const WORKSPACE_TABS: TabItem[] = [
  { value: 'apercu', label: "Vue d'ensemble" },
  { value: 'taches', label: 'Tâches' },
  { value: 'backlog', label: 'Sprints' },
  { value: 'gantt', label: 'Planning' },
  { value: 'jalons', label: 'Jalons' },
  { value: 'equipe', label: 'Équipe' },
  { value: 'risques', label: 'Risques' },
];

@Component({
  selector: 'app-project-workspace-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, ProgressBar, Tabs],
  templateUrl: './project-workspace-shell.html',
  styleUrl: './project-workspace-shell.css',
})
export class ProjectWorkspaceShell {
  readonly projectId = input.required<string>();

  protected readonly formatDateFr = formatDateFr;

  private readonly projectsService = inject(ProjectsService);
  private readonly peopleService = inject(PeopleService);
  private readonly sprintsService = inject(SprintsService);
  private readonly tasksService = inject(TasksService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly tabs = computed<TabItem[]>(() =>
    this.authService.currentUser()?.role === Role.ChefProjet ? WORKSPACE_TABS : WORKSPACE_TABS.filter((t) => t.value !== 'risques'),
  );
  protected readonly project = computed(() => this.projectsService.byId(this.projectId())());
  protected readonly sante = computed(() => {
    const p = this.project();
    return p ? SANTE_CONFIG[p.sante] : undefined;
  });
  protected readonly responsable = computed(() => {
    const p = this.project();
    return p ? this.peopleService.byId(p.responsableId) : undefined;
  });

  protected readonly sprintLabel = computed(() => {
    const sprint = sprintAAfficher(this.sprintsService.byProject(this.projectId())());
    const projet = this.project();
    return formatSprintLabel(sprint, projet?.sprintActuelIndex, projet?.sprintTotal);
  });

  /** Déclenche le chargement des tâches du projet dès l'entrée dans l'espace de travail (onglets Tâches/Sprints). */
  constructor() {
    effect(() => this.tasksService.byProject(this.projectId())());
  }

  private readonly urlSignal = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  protected readonly activeTab = computed(() => {
    const url = this.urlSignal();
    const match = this.tabs().find((t) => url.includes(`/${t.value}`));
    return match?.value ?? 'apercu';
  });

  goToTab(value: string): void {
    this.router.navigate(['/projets', this.projectId(), value]);
  }
}
