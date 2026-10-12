import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { Role } from '@core/models/role.model';

export const REPORTS_ROUTES: Routes = [
  {
    // Directeur DSI : lecture seule, il consulte les rapports diffusés par les chefs de projet (tous projets).
    path: '',
    canMatch: [() => inject(AuthService).currentUser()?.role === Role.DirecteurDsi],
    loadComponent: () => import('@features/diffusions/pages/diffusions-page').then((m) => m.DiffusionsPage),
    title: 'Rapports · GPDI',
    data: { breadcrumb: 'Rapports' },
  },
  {
    path: '',
    loadComponent: () => import('./pages/reports-page').then((m) => m.ReportsPage),
    title: 'Rapports · GPDI',
    data: { breadcrumb: 'Rapports' },
  },
];
