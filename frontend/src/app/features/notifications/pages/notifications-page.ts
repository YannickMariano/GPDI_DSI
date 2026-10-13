import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationsService } from '@core/services/notifications.service';
import { AppNotification, TypeAlerte } from '@core/models/notification.model';
import { RelativeTimePipe } from '@shared/pipes/relative-time.pipe';
import { EmptyState } from '@shared/components/empty-state/empty-state';

const TYPE_CONFIG: Record<TypeAlerte, { label: string; color: string }> = {
  blocage: { label: 'Blocage', color: 'var(--color-error)' },
  echeance: { label: 'Échéance', color: 'var(--color-alert)' },
  jalon: { label: 'Jalon', color: 'var(--color-ai)' },
  ia: { label: 'IA', color: 'var(--color-action)' },
  sprint: { label: 'Sprint', color: 'var(--color-success-text)' },
  rapport: { label: 'Rapport', color: 'var(--color-action)' },
  affectation: { label: 'Affectation', color: 'var(--color-text-secondary)' },
  tache: { label: 'Tâche', color: 'var(--color-success-text)' },
};

@Component({
  selector: 'app-notifications-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RelativeTimePipe, EmptyState],
  templateUrl: './notifications-page.html',
  styleUrl: './notifications-page.css',
})
export class NotificationsPage {
  private readonly notificationsService = inject(NotificationsService);
  private readonly router = inject(Router);

  protected readonly typeConfig = TYPE_CONFIG;
  protected readonly notifications = this.notificationsService.notifications;
  protected readonly unreadCount = this.notificationsService.unreadCount;

  constructor() {
    this.notificationsService.reload();
  }

  markAllRead(): void {
    this.notificationsService.markAllAsRead();
  }

  open(notification: AppNotification): void {
    this.notificationsService.markAsRead(notification.id);
    if (notification.entiteLink) {
      this.router.navigateByUrl(notification.entiteLink);
    }
  }
}
