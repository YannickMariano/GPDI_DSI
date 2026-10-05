import { Routes } from '@angular/router';

export const AI_ASSISTANT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/ai-assistant-page').then((m) => m.AiAssistantPage),
    title: 'Assistant IA · GPDI',
    data: { breadcrumb: 'Assistant IA' },
  },
];
