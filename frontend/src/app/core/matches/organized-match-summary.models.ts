export type MatchTemporalStatus = 'PASSE' | 'AUJOURD_HUI' | 'FUTUR';

export interface OrganizedMatchSummary {
  id: number;
  dateDebut: string;
  siteId: number;
  siteNom: string;
  terrainId: number;
  terrainNom: string;
  visibilite: string;
  statut: string;
  nbParticipants: number;
  placesRestantes: number;
  complet: boolean;
  statutTemporel: MatchTemporalStatus;
  joursAvantMatch: number | null;
  risquePenaliteJ1: boolean;
}
