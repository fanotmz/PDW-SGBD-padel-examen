export interface PublicMatch {
  id: number;
  dateDebut: string;
  heureDebut: string;
  statut?: string;
  statutTemporel?: string;
  siteId: number;
  siteNom: string;
  terrainId: number;
  terrainNom: string;
  organisateurMatricule: string;
  nbParticipants: number;
  placesRestantes: number;
  complet: boolean;
  montantParJoueur: number;
}

export interface PublicMatchesFilters {
  from?: string;
  to?: string;
  siteId?: number | null;
}
