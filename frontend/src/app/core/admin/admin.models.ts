export interface AdminInfoResponse {
  status: string;
}

export interface PendingRegistrationItem {
  userId: number;
  username: string;
  nomDemande: string;
  typeAbonnementDemande: 'GLOBAL' | 'SITE' | 'LIBRE';
  siteIdDemande: number | null;
  siteNomDemande: string | null;
  status: string;
}
