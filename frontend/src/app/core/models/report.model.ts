export type Ton = 'succes' | 'alerte' | 'erreur' | 'neutre';

export interface ReportTemplate {
  id: string;
  nom: string;
  description: string;
  icone: string;
  couleur: string;
}

export interface ReportKpi {
  libelle: string;
  valeur: string;
  delta: string;
  ton: Ton;
}

export interface ReportPuce {
  texte: string;
  ton: Ton;
}

export interface ReportColonne {
  libelle: string;
  largeur: string;
  align?: 'right';
}

export interface ReportCellule {
  texte: string;
  ton?: Ton;
  accent?: boolean;
}

export interface ReportSection {
  titre: string;
  puces?: ReportPuce[];
  table?: { colonnes: ReportColonne[]; lignes: ReportCellule[][] };
  note?: { texte: string; alerte?: boolean };
}

export interface ReportDocument {
  nom: string;
  meta: string;
  surtitre: string;
  titre: string;
  periode: string;
  pages: string;
  chiffres: ReportKpi[];
  sections: ReportSection[];
}
