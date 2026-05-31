import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthRole, LoginRequest, LoginResponse, RegisterRequest, RegisterResponse, StoredAuthContext } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorageKey = 'auth_token';
  private readonly authContextStorageKey = 'auth_context';
  private readonly authToken = signal<string | null>(localStorage.getItem(this.tokenStorageKey));
  private readonly authContext = signal<StoredAuthContext>(this.loadAuthContext());

  readonly isAuthenticatedState = computed(() => this.authToken() !== null);

  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, payload)
      .pipe(tap((response) => this.storeSession(response)));
  }

  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${environment.apiBaseUrl}/auth/register`, payload);
  }

  getToken(): string | null {
    return this.authToken();
  }

  hasRole(role: AuthRole): boolean {
    return this.authContext().roles.includes(role);
  }

  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN_GLOBAL') || this.hasRole('ROLE_ADMIN_SITE');
  }

  hasPlayerProfile(): boolean {
    return this.authContext().hasPlayerProfile;
  }

  isAuthenticated(): boolean {
    return this.isAuthenticatedState();
  }

  logout(): void {
    localStorage.removeItem(this.tokenStorageKey);
    localStorage.removeItem(this.authContextStorageKey);
    this.authToken.set(null);
    this.authContext.set(this.emptyAuthContext());
  }

  private storeSession(response: LoginResponse): void {
    const context: StoredAuthContext = {
      roles: Array.isArray(response.roles) ? response.roles.filter(isAuthRole) : [],
      hasPlayerProfile: response.hasPlayerProfile === true
    };

    localStorage.setItem(this.tokenStorageKey, response.token);
    localStorage.setItem(this.authContextStorageKey, JSON.stringify(context));
    this.authToken.set(response.token);
    this.authContext.set(context);
  }

  private loadAuthContext(): StoredAuthContext {
    const rawContext = localStorage.getItem(this.authContextStorageKey);

    if (rawContext == null) {
      return this.emptyAuthContext();
    }

    try {
      const parsed = JSON.parse(rawContext) as Partial<StoredAuthContext>;

      return {
        roles: Array.isArray(parsed.roles) ? parsed.roles.filter(isAuthRole) : [],
        hasPlayerProfile: parsed.hasPlayerProfile === true
      };
    } catch {
      return this.emptyAuthContext();
    }
  }

  private emptyAuthContext(): StoredAuthContext {
    return {
      roles: [],
      hasPlayerProfile: false
    };
  }
}

function isAuthRole(role: unknown): role is AuthRole {
  return role === 'ROLE_JOUEUR' || role === 'ROLE_ADMIN_SITE' || role === 'ROLE_ADMIN_GLOBAL';
}
