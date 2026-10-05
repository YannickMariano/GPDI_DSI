import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { ProjectsService } from '@core/services/projects.service';
import { RecommandationsProjetService } from '@core/services/recommandations-projet.service';
import { TasksService } from '@core/services/tasks.service';
import { Role, ROLE_LABELS } from '@core/models/role.model';
import { RailItem, railItemsForRole } from './rail-nav';
import { Avatar } from '@shared/components/avatar/avatar';

@Component({
  selector: 'app-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Avatar],
  host: { class: 'rail-host', '[class.rail-host--open]': 'mobileOpen()' },
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  readonly mobileOpen = input<boolean>(false);

  private readonly authService = inject(AuthService);
  private readonly projectsService = inject(ProjectsService);
  private readonly recommandationsService = inject(RecommandationsProjetService);
  private readonly tasksService = inject(TasksService);
  private readonly router = inject(Router);

  /** Projets dont l'utilisateur connecté est le chef de projet (seul cas où le badge « Assistant IA » est affiché). */
  private readonly mesProjetIds = computed(() => {
    const user = this.authService.currentUser();
    return user ? this.projectsService.projects().filter((p) => p.responsableId === user.id).map((p) => p.id) : [];
  });

  constructor() {
    effect(() => {
      for (const id of this.mesProjetIds()) this.recommandationsService.ensureLoaded(id);
    });
  }

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  protected readonly user = this.authService.currentUser;
  protected readonly roleLabel = computed(() => (this.user() ? ROLE_LABELS[this.user()!.role] : ''));
  protected readonly showProjectSwitcher = computed(() => {
    const role = this.user()?.role;
    return role === Role.ChefProjet || role === Role.MembreEquipe;
  });
  protected readonly activeProject = computed(() => {
    const id = this.user()?.projetActifId;
    return id ? this.projectsService.byId(id)() : undefined;
  });

  protected readonly railItems = computed(() => {
    const role = this.user()?.role;
    if (!role) return [];
    const items = railItemsForRole(role);
    const badges: Record<string, number> = {
      monTravail: this.tasksService.tasks().filter((t) => t.assigneIds.includes(this.user()!.id) && t.statut !== 'terminee').length,
      projetsActifs: this.projectsService.totalActifs(),
      ia: this.recommandationsService.recommandations().filter((r) => this.mesProjetIds().includes(r.projectId)).length,
    };
    return items.map((item) => ({ ...item, badge: item.badgeKey ? badges[item.badgeKey] : undefined }));
  });

  isItemActive(item: RailItem): boolean {
    const url = this.currentUrl();
    if (item.exact) return url === item.route;
    const prefixes = [item.route, ...(item.activePrefixes ?? [])];
    return prefixes.some((prefix) => url === prefix || url.startsWith(prefix));
  }

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
