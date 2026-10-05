import { Routes } from '@angular/router';

export const MY_WORK_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/my-work-page').then((m) => m.MyWorkPage),
    title: 'Mon travail · GPDI',
    data: { breadcrumb: 'Mon travail' },
  },
];
