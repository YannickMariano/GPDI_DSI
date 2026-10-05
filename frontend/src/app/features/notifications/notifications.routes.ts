import { Routes } from '@angular/router';

export const NOTIFICATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/notifications-page').then((m) => m.NotificationsPage),
    title: 'Alertes · GPDI',
    data: { breadcrumb: 'Alertes' },
  },
];
