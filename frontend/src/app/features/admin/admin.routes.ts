import { Routes } from '@angular/router';
import { roleGuard } from '@core/guards/role.guard';
import { Role } from '@core/models/role.model';

export const ADMIN_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'tableau-de-bord' },
  {
    path: 'tableau-de-bord',
    canActivate: [roleGuard(Role.Administrateur)],
    loadComponent: () => import('./pages/admin-dashboard-page').then((m) => m.AdminDashboardPage),
    title: 'Tableau de bord · Administration · GPDI',
    data: { breadcrumb: 'Tableau de bord' },
  },
  {
    path: 'comptes',
    canActivate: [roleGuard(Role.Administrateur)],
    loadComponent: () => import('./pages/users-page').then((m) => m.UsersPage),
    title: 'Comptes utilisateurs · Administration · GPDI',
    data: { breadcrumb: 'Comptes utilisateurs' },
  },
  {
    path: 'roles',
    canActivate: [roleGuard(Role.Administrateur)],
    loadComponent: () => import('./pages/roles-page').then((m) => m.RolesPage),
    title: 'Rôles et habilitations · Administration · GPDI',
    data: { breadcrumb: 'Rôles et habilitations' },
  },
  {
    path: 'equipe-projet',
    canActivate: [roleGuard(Role.Administrateur)],
    loadComponent: () => import('./pages/admin-team-page').then((m) => m.AdminTeamPage),
    title: 'Équipe de projet · Administration · GPDI',
    data: { breadcrumb: 'Équipe de projet' },
  },
  {
    path: 'journal-acces',
    // Accessible aussi au Directeur DSI, en plus de l'Administrateur (garde déjà élargie sur le parent /administration).
    loadComponent: () => import('./pages/access-log-page').then((m) => m.AccessLogPage),
    title: "Journal d'accès · Administration · GPDI",
    data: { breadcrumb: "Journal d'accès" },
  },
  {
    path: 'partage-client',
    canActivate: [roleGuard(Role.Administrateur)],
    loadComponent: () => import('./pages/client-shares-page').then((m) => m.ClientSharesPage),
    title: 'Partage client · Administration · GPDI',
    data: { breadcrumb: 'Partage client' },
  },
];
