import { Routes } from '@angular/router';
import { roleGuard } from '@core/guards/role.guard';
import { Role } from '@core/models/role.model';

export const PROJECTS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./pages/my-projects-page').then((m) => m.MyProjectsPage),
    title: 'Mes projets · GPDI',
    data: { breadcrumb: 'Mes projets' },
  },
  {
    path: ':projectId',
    loadComponent: () => import('./layout/project-workspace-shell').then((m) => m.ProjectWorkspaceShell),
    data: { breadcrumb: 'Projet' },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'apercu' },
      {
        path: 'apercu',
        loadComponent: () => import('./pages/project-overview-page').then((m) => m.ProjectOverviewPage),
        data: { breadcrumb: "Vue d'ensemble" },
      },
      {
        path: 'taches',
        loadComponent: () => import('./pages/kanban-page').then((m) => m.KanbanPage),
        data: { breadcrumb: 'Tâches' },
      },
      {
        path: 'backlog',
        loadComponent: () => import('./pages/backlog-page').then((m) => m.BacklogPage),
        data: { breadcrumb: 'Backlog' },
      },
      {
        path: 'gantt',
        loadComponent: () => import('./pages/gantt-page').then((m) => m.GanttPage),
        data: { breadcrumb: 'Planning' },
      },
      {
        path: 'jalons',
        loadComponent: () => import('./pages/milestones-page').then((m) => m.MilestonesPage),
        data: { breadcrumb: 'Jalons' },
      },
      {
        path: 'equipe',
        loadComponent: () => import('./pages/team-page').then((m) => m.TeamPage),
        data: { breadcrumb: 'Équipe' },
      },
      {
        path: 'risques',
        canActivate: [roleGuard(Role.ChefProjet)],
        loadComponent: () => import('./pages/project-risks-page').then((m) => m.ProjectRisksPage),
        data: { breadcrumb: 'Risques' },
      },
    ],
  },
];
