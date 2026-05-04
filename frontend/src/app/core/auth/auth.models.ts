export type AuthRole = 'ROLE_JOUEUR' | 'ROLE_ADMIN_SITE' | 'ROLE_ADMIN_GLOBAL';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  roles: AuthRole[];
  hasPlayerProfile: boolean;
}

export interface StoredAuthContext {
  roles: AuthRole[];
  hasPlayerProfile: boolean;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details: Record<string, string> | null;
}
