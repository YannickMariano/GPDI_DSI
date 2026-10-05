import { Sprint } from '@core/models/sprint.model';

/** Le sprint le plus pertinent à afficher : celui en cours, sinon le dernier clôturé, sinon le premier planifié. */
export function sprintAAfficher(sprints: Sprint[]): Sprint | undefined {
  const enCours = sprints.find((s) => s.statut === 'en_cours');
  if (enCours) return enCours;
  const clotures = [...sprints].filter((s) => s.statut === 'cloture').sort((a, b) => b.dateFin.localeCompare(a.dateFin));
  return clotures[0] ?? sprints.find((s) => s.statut === 'planifie');
}

function extraireNumeroSprint(nom: string): number | undefined {
  const match = nom.match(/\d+/);
  return match ? Number.parseInt(match[0], 10) : undefined;
}

function joursRestants(dateFinIso: string): number {
  const fin = new Date(`${dateFinIso}T23:59:59`);
  const diffMs = fin.getTime() - Date.now();
  return Math.max(0, Math.ceil(diffMs / (1000 * 60 * 60 * 24)));
}

/** "Sprint 4 - 8 jours restants" / "Sprint 4 - Terminé", calculé dynamiquement à partir des dates réelles du sprint. */
export function formatSprintLabel(sprint: Sprint | undefined, indexRepli?: number, totalRepli?: number): string {
  const numero = (sprint ? extraireNumeroSprint(sprint.nom) : undefined) ?? indexRepli;
  if (!sprint || numero === undefined) return `Sprint ${indexRepli ?? '—'}/${totalRepli ?? '—'}`;

  if (sprint.statut === 'cloture') return `Sprint ${numero} - Terminé`;

  const jours = joursRestants(sprint.dateFin);
  if (jours <= 0) return `Sprint ${numero} - Terminé`;
  return `Sprint ${numero} - ${jours} jour${jours > 1 ? 's' : ''} restant${jours > 1 ? 's' : ''}`;
}
