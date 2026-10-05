import { Routes } from '@angular/router';

export const RISKS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/risks-page').then((m) => m.RisksPage),
    title: 'Risques · GPDI',
    data: { breadcrumb: 'Risques' },
  },
];
