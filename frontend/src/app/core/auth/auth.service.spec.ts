import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { LoginResponse } from './auth.models';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  const loginPayload = {
    username: 'player@example.test',
    password: 'secret'
  };

  const loginResponse: LoginResponse = {
    token: 'jwt-token',
    type: 'Bearer',
    roles: ['ROLE_JOUEUR', 'ROLE_ADMIN_SITE'],
    hasPlayerProfile: true
  };

  function configureTestingModule(): void {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  }

  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    httpTesting?.verify();
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('starts unauthenticated when localStorage is empty', () => {
    configureTestingModule();

    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.hasPlayerProfile()).toBe(false);
    expect(service.isAdmin()).toBe(false);
  });

  it('calls the configured login endpoint and stores the session', () => {
    configureTestingModule();

    service.login(loginPayload).subscribe((response) => {
      expect(response).toEqual(loginResponse);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(loginPayload);

    request.flush(loginResponse);

    expect(localStorage.getItem('auth_token')).toBe('jwt-token');
    expect(JSON.parse(localStorage.getItem('auth_context') ?? '{}')).toEqual({
      roles: ['ROLE_JOUEUR', 'ROLE_ADMIN_SITE'],
      hasPlayerProfile: true
    });
    expect(service.getToken()).toBe('jwt-token');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.hasRole('ROLE_JOUEUR')).toBe(true);
    expect(service.hasRole('ROLE_ADMIN_SITE')).toBe(true);
    expect(service.hasRole('ROLE_ADMIN_GLOBAL')).toBe(false);
    expect(service.hasPlayerProfile()).toBe(true);
  });

  it('filters unsupported roles from a login response before storing context', () => {
    configureTestingModule();

    service.login(loginPayload).subscribe();

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/auth/login`);
    request.flush({
      ...loginResponse,
      roles: ['ROLE_JOUEUR', 'ROLE_INCONNU']
    });

    expect(JSON.parse(localStorage.getItem('auth_context') ?? '{}')).toEqual({
      roles: ['ROLE_JOUEUR'],
      hasPlayerProfile: true
    });
    expect(service.hasRole('ROLE_JOUEUR')).toBe(true);
    expect(service.hasRole('ROLE_ADMIN_SITE')).toBe(false);
  });

  it('clears the stored session on logout', () => {
    localStorage.setItem('auth_token', 'existing-token');
    localStorage.setItem(
      'auth_context',
      JSON.stringify({ roles: ['ROLE_ADMIN_GLOBAL'], hasPlayerProfile: true })
    );
    configureTestingModule();

    expect(service.isAuthenticated()).toBe(true);
    expect(service.hasRole('ROLE_ADMIN_GLOBAL')).toBe(true);

    service.logout();

    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(localStorage.getItem('auth_context')).toBeNull();
    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.hasRole('ROLE_ADMIN_GLOBAL')).toBe(false);
    expect(service.hasPlayerProfile()).toBe(false);
  });

  it('initializes authentication state from localStorage', () => {
    localStorage.setItem('auth_token', 'stored-token');
    localStorage.setItem(
      'auth_context',
      JSON.stringify({ roles: ['ROLE_ADMIN_GLOBAL'], hasPlayerProfile: false })
    );

    configureTestingModule();

    expect(service.getToken()).toBe('stored-token');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.hasRole('ROLE_ADMIN_GLOBAL')).toBe(true);
    expect(service.isAdmin()).toBe(true);
    expect(service.hasPlayerProfile()).toBe(false);
  });

  it('falls back to an empty auth context when localStorage context is invalid', () => {
    localStorage.setItem('auth_token', 'stored-token');
    localStorage.setItem('auth_context', '{invalid-json');

    configureTestingModule();

    expect(service.getToken()).toBe('stored-token');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.hasRole('ROLE_JOUEUR')).toBe(false);
    expect(service.isAdmin()).toBe(false);
    expect(service.hasPlayerProfile()).toBe(false);
  });
});
