export interface ChatMessage {
  id: string;
  isUser: boolean;
  texte: string;
  source?: string;
  quand: string;
}
