export interface AdminInfoResponse {
  status: string;
}

export type RegistrationSubscriptionType = 'GLOBAL' | 'SITE' | 'LIBRE';

export interface PendingRegistrationItem {
  userId: number;
  username: string;
  nomDemande: string;
  typeAbonnementDemande: RegistrationSubscriptionType;
  siteIdDemande: number | null;
  siteNomDemande: string | null;
  status: string;
}

export interface ValidateRegistrationRequest {
  typeAbonnementFinal: RegistrationSubscriptionType;
  siteIdFinal?: number;
}

export interface RegistrationDecisionResponse {
  userId: number;
  username: string;
  status: string;
  message: string;
  joueurMatricule: string | null;
  joueurNom: string | null;
  joueurType: RegistrationSubscriptionType | null;
  joueurSiteId: number | null;
}
