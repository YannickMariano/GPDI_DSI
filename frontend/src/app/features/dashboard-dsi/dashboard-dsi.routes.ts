import { Routes } from '@angular/router';

export const DASHBOARD_DSI_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/dashboard-dsi-page').then((m) => m.DashboardDsiPage),
    title: 'Tableau de bord · GPDI',
    data: { breadcrumb: 'Tableau de bord' },
  },
];
