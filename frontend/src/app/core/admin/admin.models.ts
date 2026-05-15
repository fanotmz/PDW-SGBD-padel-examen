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
  penaliteJusqua?: string | null;
}

export interface AdminSitePlayerResponse {
  matricule: string;
  nom: string;
  type: RegistrationSubscriptionType;
  solde: number | null;
  penaliteJusqua?: string | null;
}

export interface AdminPlayerListItem {
  matricule: string;
  nom: string;
  type: RegistrationSubscriptionType;
  siteId: number | null;
  siteNom: string | null;
  solde: number | null;
  penaliteJusqua?: string | null;
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

export interface AdminSiteScheduleResponse {
  id: number;
  siteId: number;
  annee: number;
  heureOuverture: string;
  heureFermeture: string;
}

export interface AdminSiteConsultationTerrain {
  id: number;
  nom: string;
  siteId: number;
}

export interface AdminSiteConsultationResponse {
  id: number;
  nom: string;
  ville: string;
  joursFermeture: string[];
  terrains: AdminSiteConsultationTerrain[];
  horaires: AdminSiteScheduleResponse[];
}

export interface AdminSiteMatchSummaryResponse {
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
  peutAnnuler: boolean;
}

export interface AdminCaStatsResponse {
  caTotal: number;
  from: string;
  to: string;
}

export interface AdminMatchsStatsResponse {
  nbMatchs: number;
  from: string;
  to: string;
}

export interface AdminDettesStatsResponse {
  detteTotale: number;
  nbJoueursEnDette: number;
}

export interface UpsertSiteScheduleRequest {
  annee: number;
  heureOuverture: string;
  heureFermeture: string;
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

export interface UpdateSiteDateClosureRequest {
  date: string;
  motif?: string | null;
}

export interface UpdateSitePeriodClosureRequest {
  dateDebut: string;
  dateFin: string;
  motif?: string | null;
}
