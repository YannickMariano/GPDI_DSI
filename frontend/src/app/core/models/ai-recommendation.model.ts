export interface ChatMessage {
  id: string;
  isUser: boolean;
  texte: string;
  source?: string;
  quand: string;
  /** Action proposée par l'assistant global : rien n'est exécuté avant la confirmation de l'utilisateur. */
  action?: ActionAssistant;
}

export type StatutActionAssistant = 'PROPOSEE' | 'APPLIQUEE' | 'ANNULEE';

export interface ActionAssistant {
  id: string;
  type: string;
  libelle: string;
  details: string[];
  /** Action définitive (suppression, clôture ou archivage de projet). */
  irreversible: boolean;
  statut: StatutActionAssistant;
  resultat?: string;
  idProjet?: string;
}
