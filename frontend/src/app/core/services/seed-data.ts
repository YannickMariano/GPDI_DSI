import { ReportTemplate } from '@core/models/report.model';

export const SEED_REPORT_TEMPLATES: ReportTemplate[] = [
  { id: 'hebdo', nom: 'Avancement hebdomadaire', description: 'Synthèse projet diffusée chaque lundi au comité', icone: '▦', couleur: 'var(--color-action)' },
  { id: 'sprint', nom: 'Bilan de sprint', description: 'Vélocité, engagements tenus, causes de report', icone: '◫', couleur: 'var(--color-success)' },
  { id: 'risques', nom: 'Registre des risques', description: 'État consolidé des risques et de leurs parades', icone: '◭', couleur: 'var(--color-error)' },
  { id: 'temps', nom: 'Consommation des temps', description: 'Heures saisies par agent, par tâche et par projet', icone: '◑', couleur: 'var(--color-alert)' },
  { id: 'portefeuille', nom: 'Portefeuille DSI', description: 'Vue consolidée des projets de la direction', icone: '▤', couleur: 'var(--color-ai)' },
  { id: 'predictif', nom: 'Analyse prédictive', description: 'Projection de fin de projet et alertes de l\'assistant', icone: '✦', couleur: 'var(--color-ai)' },
];
