import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { AuthUser, LoginCredentials, LoginResult } from '@core/models/user.model';
import { avatarColorFor } from '@shared/utils/avatar-color';
import { UtilisateurResponse, initialesUtilisateur, nomAffiche, roleDepuisNom } from '@shared/utils/utilisateur-view';

const STORAGE_KEY = 'gpdi.session';
const TOKEN_ENDPOINT = `${environment.keycloak.issuer}/protocol/openid-connect/token`;
const LOGOUT_ENDPOINT = `${environment.keycloak.issuer}/protocol/openid-connect/logout`;
/** Marge appliquée à l'expiration du jeton pour lancer le rafraîchissement un peu avant l'échéance réelle. */
const MARGE_EXPIRATION_MS = 10_000;

interface KeycloakTokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

interface KeycloakTokenError {
  error: string;
  error_description?: string;
}

interface StoredSession {
  user: AuthUser;
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private readonly _currentUser = signal<AuthUser | null>(null);
  private readonly _sessionToken = signal<string | null>(null);
  private refreshToken: string | null = null;
  private expiresAt = 0;
  private gardeSession = true;

  readonly currentUser = this._currentUser.asReadonly();
  readonly sessionToken = this._sessionToken.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null);

  /** Restaure la session depuis le stockage local au démarrage ; rafraîchit le jeton si expiré. Attendu par `provideAppInitializer`. */
  async restoreFromStorage(): Promise<void> {
    if (!this.isBrowser) return;

    const raw = localStorage.getItem(STORAGE_KEY) ?? sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return;

    try {
      const session = JSON.parse(raw) as StoredSession;
      this.gardeSession = localStorage.getItem(STORAGE_KEY) !== null;

      if (session.expiresAt - MARGE_EXPIRATION_MS > Date.now()) {
        this.applySession(session);
        return;
      }

      const rafraichi = await this.tryRefresh(session.refreshToken, session.user);
      if (!rafraichi) this.clearStorage();
    } catch {
      this.clearStorage();
    }
  }

  async login(credentials: LoginCredentials): Promise<LoginResult> {
    const { identifiant, motDePasse, gardeSession } = credentials;

    const body = new URLSearchParams({
      grant_type: 'password',
      client_id: environment.keycloak.clientId,
      username: identifiant.trim(),
      password: motDePasse,
      scope: 'openid',
    });

    let jeton: KeycloakTokenResponse;
    try {
      jeton = await firstValueFrom(
        this.http.post<KeycloakTokenResponse>(TOKEN_ENDPOINT, body, {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        }),
      );
    } catch (erreur) {
      return { success: false, message: this.messageEchecConnexion(erreur) };
    }

    this._sessionToken.set(jeton.access_token);

    let profil: UtilisateurResponse;
    try {
      profil = await firstValueFrom(this.http.get<UtilisateurResponse>(`${environment.apiUrl}/utilisateurs/moi`));
    } catch (erreur) {
      this._sessionToken.set(null);
      if (erreur instanceof HttpErrorResponse && erreur.status === 403) {
        return { success: false, message: erreur.error?.message ?? 'Votre compte a été suspendu. Demandez à votre supérieur de réactiver votre compte.' };
      }
      return { success: false, message: "Connexion au serveur GPDI impossible. Réessayez ultérieurement." };
    }

    const user = this.versAuthUser(profil);
    this.gardeSession = gardeSession;
    this.refreshToken = jeton.refresh_token;
    this.expiresAt = Date.now() + jeton.expires_in * 1000;
    this._currentUser.set(user);
    this.persist(user, jeton.access_token, jeton.refresh_token, this.expiresAt);

    return { success: true, user };
  }

  logout(): void {
    const refreshToken = this.refreshToken;
    if (this.isBrowser && refreshToken) {
      const body = new URLSearchParams({ client_id: environment.keycloak.clientId, refresh_token: refreshToken });
      // Best-effort : révoque la session Keycloak sans bloquer la déconnexion locale en cas d'échec réseau.
      firstValueFrom(this.http.post(LOGOUT_ENDPOINT, body, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })).catch(() => undefined);
    }

    this._currentUser.set(null);
    this._sessionToken.set(null);
    this.refreshToken = null;
    this.expiresAt = 0;
    this.clearStorage();
  }

  /** Rafraîchit le jeton d'accès à partir du refresh token courant. Utilisé par l'intercepteur HTTP sur un 401. */
  async refreshSession(): Promise<boolean> {
    const user = this._currentUser();
    if (!this.refreshToken || !user) return false;
    return this.tryRefresh(this.refreshToken, user);
  }

  private async tryRefresh(refreshToken: string, user: AuthUser): Promise<boolean> {
    const body = new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: environment.keycloak.clientId,
      refresh_token: refreshToken,
    });

    try {
      const jeton = await firstValueFrom(
        this.http.post<KeycloakTokenResponse>(TOKEN_ENDPOINT, body, {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        }),
      );
      this.expiresAt = Date.now() + jeton.expires_in * 1000;
      this.refreshToken = jeton.refresh_token;
      this.applySession({ user, accessToken: jeton.access_token, refreshToken: jeton.refresh_token, expiresAt: this.expiresAt });
      return true;
    } catch {
      return false;
    }
  }

  private applySession(session: StoredSession): void {
    this._currentUser.set(session.user);
    this._sessionToken.set(session.accessToken);
    this.refreshToken = session.refreshToken;
    this.expiresAt = session.expiresAt;
    this.persist(session.user, session.accessToken, session.refreshToken, session.expiresAt);
  }

  private versAuthUser(profil: UtilisateurResponse): AuthUser {
    const role = roleDepuisNom(profil.nomRole);
    if (!role) {
      throw new Error(`Rôle applicatif inconnu ou non configuré côté DSI : "${profil.nomRole}".`);
    }
    return {
      id: String(profil.id),
      matricule: profil.matricule,
      nom: nomAffiche(profil.prenom, profil.nom),
      initiales: initialesUtilisateur(profil.prenom, profil.nom),
      avatarBg: avatarColorFor(profil.matricule),
      role,
      direction: profil.specialite ?? '',
    };
  }

  private messageEchecConnexion(erreur: unknown): string {
    const serveurInjoignable = "Connexion au serveur d'authentification impossible. Réessayez ultérieurement.";
    if (!(erreur instanceof HttpErrorResponse)) return serveurInjoignable;

    // status 0 : requête jamais parvenue au serveur (réseau, CORS, Keycloak indisponible) — à distinguer d'un vrai refus d'identifiants.
    if (erreur.status === 0) return serveurInjoignable;

    const description = (erreur.error as KeycloakTokenError | undefined)?.error_description;
    if (description === 'Account disabled') {
      return 'Ce compte est désactivé. Contactez votre administrateur.';
    }
    if (erreur.status === 400 || erreur.status === 401) {
      return 'Identifiants incorrects.';
    }
    return serveurInjoignable;
  }

  private persist(user: AuthUser, accessToken: string, refreshToken: string, expiresAt: number): void {
    if (!this.isBrowser) return;
    const session: StoredSession = { user, accessToken, refreshToken, expiresAt };
    const store = this.gardeSession ? localStorage : sessionStorage;
    store.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  private clearStorage(): void {
    if (!this.isBrowser) return;
    localStorage.removeItem(STORAGE_KEY);
    sessionStorage.removeItem(STORAGE_KEY);
  }
}
