export function formatDateFr(iso: string | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function formatDateCourteFr(iso: string | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
}

const HEURES_PAR_JOUR = 24;

/** Au-delà de 24 h, les jours sont affichés (« 2 j 5 h », « 3 j ») : jamais plus de 24 h dans la partie heures. */
export function formatHours(hours: number | undefined): string {
  if (hours === undefined) return '–';
  const signe = hours < 0 ? '-' : '';
  const absolu = Math.abs(hours);
  if (absolu < HEURES_PAR_JOUR) return `${signe}${absolu.toString().replace('.', ',')} h`;
  const jours = Math.floor(absolu / HEURES_PAR_JOUR);
  // Arrondi au centième pour éviter les artefacts de calcul flottant sur le reste (ex. 0,4999999).
  const reste = Math.round((absolu - jours * HEURES_PAR_JOUR) * 100) / 100;
  return reste === 0 ? `${signe}${jours} j` : `${signe}${jours} j ${reste.toString().replace('.', ',')} h`;
}

/** Convertit une durée en heures décimales au format "HH:MM:SS" ; au-delà de 24 h, les jours sont ajoutés devant ("2 j 05:20:15"). */
export function formatHms(hours: number): string {
  const signe = hours < 0 ? '-' : '';
  const totalSecondes = Math.round(Math.abs(hours) * 3600);
  const j = Math.floor(totalSecondes / (HEURES_PAR_JOUR * 3600));
  const h = Math.floor((totalSecondes % (HEURES_PAR_JOUR * 3600)) / 3600);
  const m = Math.floor((totalSecondes % 3600) / 60);
  const s = totalSecondes % 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${signe}${j > 0 ? `${j} j ` : ''}${pad(h)}:${pad(m)}:${pad(s)}`;
}

export function formatPercent(value: number): string {
  return `${Math.round(value)} %`;
}
