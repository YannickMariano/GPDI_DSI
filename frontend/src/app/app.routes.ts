import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
import { roleGuard } from '@core/guards/role.guard';
import { Role } from '@core/models/role.model';

export const routes: Routes = [
  {
    path: 'login',
    loadChildren: () => import('@features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'acces-refuse',
    loadComponent: () => import('@layout/access-denied/access-denied').then((m) => m.AccessDenied),
  },
  {
    path: '',
    loadComponent: () => import('@layout/shell/shell').then((m) => m.Shell),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('@features/home/home-redirect-page').then((m) => m.HomeRedirectPage) },
      {
        path: 'tableau-de-bord',
        canActivate: [roleGuard(Role.DirecteurDsi)],
        loadChildren: () => import('@features/dashboard-dsi/dashboard-dsi.routes').then((m) => m.DASHBOARD_DSI_ROUTES),
      },
      {
        path: 'portefeuille',
        canActivate: [roleGuard(Role.DirecteurDsi, Role.Administrateur)],
        loadChildren: () => import('@features/portfolio/portfolio.routes').then((m) => m.PORTFOLIO_ROUTES),
      },
      {
        path: 'projets',
        canActivate: [roleGuard(Role.ChefProjet, Role.MembreEquipe)],
        loadChildren: () => import('@features/projects/projects.routes').then((m) => m.PROJECTS_ROUTES),
      },
      {
        path: 'espace-client',
        canActivate: [roleGuard(Role.DirectionMetier)],
        loadChildren: () => import('@features/client-portal/client-portal.routes').then((m) => m.CLIENT_PORTAL_ROUTES),
      },
      {
        path: 'pilotage',
        canActivate: [roleGuard(Role.ChefProjet)],
        loadComponent: () => import('@features/projects/pages/pilotage-redirect-page').then((m) => m.PilotageRedirectPage),
      },
      {
        path: 'mon-travail',
        canActivate: [roleGuard(Role.ChefProjet, Role.MembreEquipe)],
        loadChildren: () => import('@features/my-work/my-work.routes').then((m) => m.MY_WORK_ROUTES),
      },
      {
        path: 'temps',
        canActivate: [roleGuard(Role.ChefProjet, Role.MembreEquipe)],
        loadChildren: () => import('@features/time-tracking/time-tracking.routes').then((m) => m.TIME_TRACKING_ROUTES),
      },
      {
        path: 'assistant-ia',
        canActivate: [roleGuard(Role.ChefProjet)],
        loadChildren: () => import('@features/ai-assistant/ai-assistant.routes').then((m) => m.AI_ASSISTANT_ROUTES),
      },
      {
        path: 'rapports',
        canActivate: [roleGuard(Role.ChefProjet, Role.DirecteurDsi)],
        loadChildren: () => import('@features/reports/reports.routes').then((m) => m.REPORTS_ROUTES),
      },
      {
        path: 'diffusions',
        canActivate: [roleGuard(Role.MembreEquipe)],
        loadChildren: () => import('@features/diffusions/diffusions.routes').then((m) => m.DIFFUSIONS_ROUTES),
      },
      {
        path: 'alertes',
        loadChildren: () => import('@features/notifications/notifications.routes').then((m) => m.NOTIFICATIONS_ROUTES),
      },
      {
        path: 'administration',
        canActivate: [roleGuard(Role.Administrateur, Role.DirecteurDsi)],
        loadChildren: () => import('@features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
      },
      { path: '**', loadComponent: () => import('@layout/access-denied/access-denied').then((m) => m.AccessDenied) },
    ],
  },
  { path: '**', redirectTo: '' },
];
