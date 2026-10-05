import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/login-page').then((m) => m.LoginPage),
    title: 'Connexion · GPDI',
  },
];
