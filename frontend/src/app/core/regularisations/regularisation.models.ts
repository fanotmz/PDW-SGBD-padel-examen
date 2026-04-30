export type RegularisationRole = 'ORGANISATEUR' | 'PARTICIPANT';

export interface Regularisation {
  participationId: number;
  matchId: number;
  dateMatch: string;
  siteNom: string;
  terrainNom: string;
  visibilite: string;
  roleJoueur: RegularisationRole;
  origineType: string;
  montantInitial: number;
  montantDejaPaye: number;
  montantRestant: number;
  description: string | null;
  payable: boolean;
}

export interface RegularisationsResponse {
  totalTracable: number;
  items: Regularisation[];
}
