export interface MatchParticipation {
  id: number;
  matchId: number;
  joueurMatricule: string;
}

export interface AddPrivatePlayerToMatchRequest {
  joueurMatriculeAAjouter: string;
}
