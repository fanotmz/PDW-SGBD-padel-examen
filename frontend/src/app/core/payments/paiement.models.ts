export interface PayParticipationRequest {
  montant: number;
}

export interface Paiement {
  id: number;
  participationId: number;
  montant: number;
  datePaiement: string;
}
