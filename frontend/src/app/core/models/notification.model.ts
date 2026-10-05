export type TypeAlerte = 'blocage' | 'echeance' | 'jalon' | 'ia';

export interface AppNotification {
  id: string;
  type: TypeAlerte;
  titre: string;
  meta: string;
  quand: string;
  lu: boolean;
  entiteLink?: string;
}
