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

export function formatHours(hours: number | undefined): string {
  if (hours === undefined) return '–';
  return `${hours.toString().replace('.', ',')} h`;
}

/** Convertit une durée en heures décimales au format "HH:MM:SS". */
export function formatHms(hours: number): string {
  const signe = hours < 0 ? '-' : '';
  const totalSecondes = Math.round(Math.abs(hours) * 3600);
  const h = Math.floor(totalSecondes / 3600);
  const m = Math.floor((totalSecondes % 3600) / 60);
  const s = totalSecondes % 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${signe}${pad(h)}:${pad(m)}:${pad(s)}`;
}

export function formatPercent(value: number): string {
  return `${Math.round(value)} %`;
}
