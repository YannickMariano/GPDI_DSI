import { Routes } from '@angular/router';

export const DIFFUSIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/diffusions-page').then((m) => m.DiffusionsPage),
    title: 'Diffusion · GPDI',
    data: { breadcrumb: 'Diffusion' },
  },
];
