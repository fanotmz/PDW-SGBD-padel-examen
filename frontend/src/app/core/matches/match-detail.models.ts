export interface MatchParticipant {
  matricule: string;
  nom: string;
}

export interface MatchDetail {
  id: number;
  dateDebut: string;
  heureDebut: string;
  siteId: number;
  siteNom: string;
  terrainId: number;
  terrainNom: string;
  organisateurMatricule: string;
  organisateurNom: string;
  visibilite: string;
  statut: string;
  nbParticipants: number;
  placesRestantes: number;
  complet: boolean;
  peutAjouterJoueurPrive: boolean;
  montantTotal: number;
  montantPaye: number;
  resteAPayer: number;
  montantRembourse: number;
  participants: MatchParticipant[];
}
