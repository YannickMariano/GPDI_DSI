import { Routes } from '@angular/router';

export const PORTFOLIO_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/portfolio-page').then((m) => m.PortfolioPage),
    title: 'Portefeuille · GPDI',
    data: { breadcrumb: 'Portefeuille' },
  },
];
