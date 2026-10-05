import { Routes } from '@angular/router';

export const TIME_TRACKING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/time-tracking-page').then((m) => m.TimeTrackingPage),
    title: 'Saisie des temps · GPDI',
    data: { breadcrumb: 'Saisie des temps' },
  },
];
