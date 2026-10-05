import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { SANS_TOAST_ERREUR } from '@core/interceptors/error.interceptor';
import { AppNotification, TypeAlerte } from '@core/models/notification.model';
import { AuthService } from './auth.service';

interface Page<T> {
  content: T[];
}

interface NotificationResponse {
  id: number;
  type: string;
  titre: string;
  meta: string | null;
  lien: string | null;
  lu: boolean;
  dateCreation: string;
}

const INTERVALLE_ACTUALISATION_MS = 60_000;

function versNotification(dto: NotificationResponse): AppNotification {
  return {
    id: String(dto.id),
    type: dto.type.toLowerCase() as TypeAlerte,
    titre: dto.titre,
    meta: dto.meta ?? '',
    quand: dto.dateCreation,
    lu: dto.lu,
    entiteLink: dto.lien ?? undefined,
  };
}

/** Notifications personnelles de l'utilisateur connecté (entité backend `Notification`, `/api/notifications`). */
@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  private readonly _notifications = signal<AppNotification[]>([]);
  readonly notifications = this._notifications.asReadonly();
  readonly unreadCount = computed(() => this._notifications().filter((n) => !n.lu).length);

  private readonly idUtilisateur = computed(() => this.authService.currentUser()?.id ?? null);

  constructor() {
    // Charge à la connexion, et repart de zéro quand l'utilisateur change ou se déconnecte.
    effect(() => {
      if (this.idUtilisateur()) this.reload();
      else this._notifications.set([]);
    });
    setInterval(() => {
      if (this.idUtilisateur()) this.reload(true);
    }, INTERVALLE_ACTUALISATION_MS);
  }

  /** Recharge les notifications ; `silencieux` évite d'afficher une alerte si le serveur est momentanément injoignable (actualisation automatique). */
  reload(silencieux = false): void {
    const context = silencieux ? new HttpContext().set(SANS_TOAST_ERREUR, true) : undefined;
    firstValueFrom(this.http.get<Page<NotificationResponse>>(`${environment.apiUrl}/notifications`, { params: { size: '100' }, context }))
      .then((page) => this._notifications.set(page.content.map(versNotification)))
      .catch(() => undefined);
  }

  markAsRead(id: string): void {
    const cible = this._notifications().find((n) => n.id === id);
    if (!cible || cible.lu) return;
    this._notifications.update((list) => list.map((n) => (n.id === id ? { ...n, lu: true } : n)));
    firstValueFrom(this.http.post<unknown>(`${environment.apiUrl}/notifications/${id}/lire`, {})).catch(() => this.reload());
  }

  markAllAsRead(): void {
    if (this.unreadCount() === 0) return;
    this._notifications.update((list) => list.map((n) => ({ ...n, lu: true })));
    firstValueFrom(this.http.post<unknown>(`${environment.apiUrl}/notifications/lire-toutes`, {})).catch(() => this.reload());
  }
}
