import { ChangeDetectionStrategy, Component, computed, inject, output, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { NotificationsService } from '@core/services/notifications.service';
import { ProjectsService } from '@core/services/projects.service';
import { RelativeTimePipe } from '@shared/pipes/relative-time.pipe';
import { ClickOutside } from '@shared/directives/click-outside';
import { SearchBar } from '@shared/components/search-bar/search-bar';
import { ToastService } from '@shared/components/toast/toast.service';
import { AppNotification, TypeAlerte } from '@core/models/notification.model';
import { Role } from '@core/models/role.model';

const ALERT_ICON: Record<TypeAlerte, string> = {
  blocage: '⛔',
  echeance: '◔',
  jalon: '◆',
  ia: '✦',
  sprint: '▶',
  rapport: '▦',
  affectation: '◍',
  tache: '◫',
};

@Component({
  selector: 'app-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RelativeTimePipe, ClickOutside, SearchBar],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  readonly menuToggle = output<void>();

  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly notificationsService = inject(NotificationsService);
  private readonly authService = inject(AuthService);

  /** Seul le chef de projet a accès à l'assistant IA (route /assistant-ia gardée par roleGuard(Role.ChefProjet)). */
  protected readonly peutAccederAssistantIa = computed(() => this.authService.currentUser()?.role === Role.ChefProjet);

  protected readonly notifications = this.notificationsService.notifications;
  protected readonly unreadCount = this.notificationsService.unreadCount;
  protected readonly notificationsOpen = signal(false);

  protected toggleNotifications(): void {
    const ouvrir = !this.notificationsOpen();
    this.notificationsOpen.set(ouvrir);
    if (ouvrir) this.notificationsService.reload();
  }
  protected readonly alertIcon = ALERT_ICON;
  protected readonly searchTerm = signal('');

  private readonly projectsService = inject(ProjectsService);
  private readonly toast = inject(ToastService);

  private readonly navigationTrigger = toSignal(
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)),
    { initialValue: null },
  );

  protected readonly breadcrumbs = computed(() => {
    this.navigationTrigger();
    const crumbs: { label: string; url: string }[] = [];
    let route: ActivatedRoute | null = this.activatedRoute.root;
    let url = '';

    while (route) {
      const child: ActivatedRoute | null = route.firstChild;
      if (!child) break;
      const segment = child.snapshot.url.map((s) => s.path).join('/');
      if (segment) url += `/${segment}`;
      const label = child.snapshot.data['breadcrumb'] as string | undefined;
      if (label) crumbs.push({ label, url });
      route = child;
    }

    return crumbs;
  });

  protected openNotification(notification: AppNotification): void {
    this.notificationsService.markAsRead(notification.id);
    this.notificationsOpen.set(false);
    if (notification.entiteLink) {
      this.router.navigateByUrl(notification.entiteLink);
    }
  }

  protected search(): void {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return;
    const match = this.projectsService.projects().find((p) => p.nom.toLowerCase().includes(term));
    if (match) {
      this.router.navigate(['/projets', match.id, 'apercu']);
      this.searchTerm.set('');
    } else {
      this.toast.show({ type: 'info', message: `Aucun projet ne correspond à « ${this.searchTerm()} ».` });
    }
  }
}
