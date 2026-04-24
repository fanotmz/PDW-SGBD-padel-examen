import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorageKey = 'auth_token';
  private readonly authToken = signal<string | null>(localStorage.getItem(this.tokenStorageKey));

  readonly isAuthenticatedState = computed(() => this.authToken() !== null);

  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, payload)
      .pipe(tap((response) => this.storeToken(response.token)));
  }

  storeToken(token: string): void {
    localStorage.setItem(this.tokenStorageKey, token);
    this.authToken.set(token);
  }

  getToken(): string | null {
    return this.authToken();
  }

  isAuthenticated(): boolean {
    return this.isAuthenticatedState();
  }

  logout(): void {
    localStorage.removeItem(this.tokenStorageKey);
    this.authToken.set(null);
  }
}
