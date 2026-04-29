export type PlayerType = 'GLOBAL' | 'SITE' | 'LIBRE';

export interface MeProfile {
  matricule: string;
  nom: string;
  type: PlayerType;
  siteId: number | null;
  solde: number;
}
