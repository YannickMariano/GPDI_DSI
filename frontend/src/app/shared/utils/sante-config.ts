import { SanteProjet } from '@core/models/project.model';

export const SANTE_CONFIG: Record<SanteProjet, { label: string; bg: string; text: string; border: string }> = {
  sain: { label: 'Sain', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)', border: 'var(--color-success)' },
  sous_surveillance: { label: 'Sous surveillance', bg: 'var(--priority-haute-bg)', text: 'var(--priority-haute-text)', border: 'var(--color-alert)' },
  a_risque: { label: 'À risque', bg: 'var(--status-bloquee-bg)', text: 'var(--status-bloquee-text)', border: 'var(--color-error)' },
};
