export type PlayerType = 'GLOBAL' | 'SITE' | 'LIBRE';

export interface MeProfile {
  matricule: string;
  nom: string;
  type: PlayerType;
  siteId: number | null;
  solde: number;
  penaliteJusqua?: string | null;
}

export interface MeStats {
  prochainMatch: MeNextMatch | null;
  matchsCommeOrganisateur: MeMatchRoleStats;
  matchsCommeParticipant: MeMatchRoleStats;
  paiements: MePaymentStats;
}

export interface MeNextMatch {
  id: number;
  dateDebut: string;
  siteNom: string;
  terrainNom: string;
  roleJoueur: 'ORGANISATEUR' | 'PARTICIPANT';
}

export interface MeMatchRoleStats {
  joues: number;
  aVenir: number;
  annules: number;
}

export interface MePaymentStats {
  participationsPayees: number;
  participationsAPayer: number;
  montantNetPaye: number;
  montantRembourse: number;
}
