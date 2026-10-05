import { Routes } from '@angular/router';

export const CLIENT_PORTAL_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'avancement' },
  {
    path: 'avancement',
    loadComponent: () => import('./pages/shared-progress-page').then((m) => m.SharedProgressPage),
    title: 'Avancement partagé · GPDI',
    data: { breadcrumb: 'Avancement partagé' },
  },
  {
    path: 'etapes',
    loadComponent: () => import('./pages/key-steps-page').then((m) => m.KeyStepsPage),
    title: 'Étapes clés · GPDI',
    data: { breadcrumb: 'Étapes clés' },
  },
  {
    path: 'informations',
    loadComponent: () => import('./pages/info-points-page').then((m) => m.InfoPointsPage),
    title: "Points d'information · GPDI",
    data: { breadcrumb: "Points d'information" },
  },
];
