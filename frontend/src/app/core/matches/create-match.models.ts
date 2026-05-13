export type MatchVisibility = 'PUBLIC' | 'PRIVE';

export interface CreateMatchPayload {
  terrainId: number;
  dateDebut: string;
  visibilite: MatchVisibility;
}

export interface CreatedMatch {
  id: number;
  terrainId: number;
  terrainNom: string;
  siteId: number;
  organisateurMatricule: string;
  dateDebut: string;
  visibilite: string;
  statut: string;
  nbParticipants: number;
  montantTotal: number;
  montantPaye: number;
  resteAPayer: number;
}

export interface MatchSlotsResponse {
  creneaux: string[];
  message: string | null;
}
