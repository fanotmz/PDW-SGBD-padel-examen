export interface AdminInfoResponse {
  status: string;
  adminType: 'GLOBAL' | 'SITE';
  siteId: number | null;
  siteNom: string | null;
}

export type RegistrationSubscriptionType = 'GLOBAL' | 'SITE' | 'LIBRE';

export interface AdminPlayerResponse {
  matricule: string;
  nom: string;
  type: RegistrationSubscriptionType;
  siteId: number | null;
  solde: number | null;
}

export interface AdminSitePlayerResponse {
  matricule: string;
  nom: string;
  type: RegistrationSubscriptionType;
  solde: number | null;
}

export interface AdminPlayerListItem {
  matricule: string;
  nom: string;
  type: RegistrationSubscriptionType;
  siteId: number | null;
  siteNom: string | null;
  solde: number | null;
}

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

export interface AdminGlobalClosureResponse {
  id: number;
  date: string;
  motif: string | null;
}

export interface AdminSiteClosureResponse {
  id: number;
  siteId: number;
  date: string | null;
  dateDebut: string | null;
  dateFin: string | null;
  motif: string | null;
}

export interface CreateGlobalClosureRequest {
  date: string;
  motif?: string | null;
}

export interface CreateSiteDateClosureRequest {
  date: string;
  motif?: string | null;
}

export interface CreateSitePeriodClosureRequest {
  dateDebut: string;
  dateFin: string;
  motif?: string | null;
}
