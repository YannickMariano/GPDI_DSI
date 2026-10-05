import { Task } from '@core/models/task.model';
import { Person } from '@core/models/person.model';

export interface MemberWorkload {
  person: Person;
  heuresAffectees: number;
  tachesEnCours: number;
  tachesBloquees: number;
  chargePct: number;
}

const CAPACITE_HEBDO_H = 42;

export function computeWorkload(tasks: Task[], people: Person[]): MemberWorkload[] {
  const byPerson = new Map<string, { heures: number; enCours: number; bloquees: number }>();

  for (const task of tasks) {
    if (task.statut === 'terminee') continue;
    for (const personId of task.assigneIds) {
      const entry = byPerson.get(personId) ?? { heures: 0, enCours: 0, bloquees: 0 };
      entry.heures += task.estimeHeures;
      if (task.statut === 'en_cours') entry.enCours++;
      if (task.statut === 'bloquee') entry.bloquees++;
      byPerson.set(personId, entry);
    }
  }

  return [...byPerson.entries()]
    .map(([personId, entry]) => {
      const person = people.find((p) => p.id === personId);
      if (!person) return null;
      return {
        person,
        heuresAffectees: entry.heures,
        tachesEnCours: entry.enCours,
        tachesBloquees: entry.bloquees,
        // Plafonnée à 100 % : au-delà, le membre est simplement « Surchargé », le pourcentage ne va pas plus loin.
        chargePct: Math.min(100, Math.round((entry.heures / CAPACITE_HEBDO_H) * 100)),
      } satisfies MemberWorkload;
    })
    .filter((w): w is MemberWorkload => w !== null)
    .sort((a, b) => b.chargePct - a.chargePct);
}

export function etatCharge(chargePct: number): { label: string; bg: string; text: string } {
  if (chargePct >= 100) return { label: 'Surchargé', bg: 'var(--status-bloquee-bg)', text: 'var(--status-bloquee-text)' };
  if (chargePct >= 90) return { label: 'Limite', bg: 'var(--priority-haute-bg)', text: 'var(--priority-haute-text)' };
  if (chargePct >= 50) return { label: 'Optimal', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)' };
  return { label: 'Disponible', bg: 'var(--status-en-cours-bg)', text: 'var(--status-en-cours-text)' };
}
