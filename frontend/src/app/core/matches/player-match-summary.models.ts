export type PlayerMatchRole = 'ORGANISATEUR' | 'PARTICIPANT';

export type MatchTemporalStatus = 'PASSE' | 'AUJOURD_HUI' | 'FUTUR';

export interface PlayerMatchSummary {
  id: number;
  dateDebut: string;
  siteId: number;
  siteNom: string;
  terrainId: number;
  terrainNom: string;
  visibilite: string;
  statut: string;
  roleJoueur: PlayerMatchRole;
  statutTemporel: MatchTemporalStatus;
  joursAvantMatch: number | null;
  paiementJoueurEffectue: boolean;
}
