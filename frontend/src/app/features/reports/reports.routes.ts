import { Routes } from '@angular/router';

export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/reports-page').then((m) => m.ReportsPage),
    title: 'Rapports · GPDI',
    data: { breadcrumb: 'Rapports' },
  },
];
