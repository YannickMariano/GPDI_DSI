import { Task } from '@core/models/task.model';

/** Temps passé « en direct » : la valeur figée en base, plus le temps écoulé depuis le démarrage si le chronomètre tourne. */
export function tempsPasseActuel(task: Task, maintenantMs: number): number {
  if (!task.demarreeLe) return task.tempsPasseHeures;
  const debutMs = Date.parse(task.demarreeLe);
  return task.tempsPasseHeures + Math.max(0, (maintenantMs - debutMs) / 3_600_000);
}
