import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { LoginPageComponent } from './login-page.component';

describe('LoginPageComponent integration', () => {
  let fixture: ComponentFixture<LoginPageComponent>;
  let authService: {
    login: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
    hasPlayerProfile: ReturnType<typeof vi.fn>;
  };
  let router: {
    navigateByUrl: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authService = {
      login: vi.fn(),
      isAdmin: vi.fn(),
      hasPlayerProfile: vi.fn()
    };
    router = {
      navigateByUrl: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPageComponent);
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('does not call login when the form is invalid', () => {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;

    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(authService.login).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain("L'identifiant est obligatoire.");
    expect(fixture.nativeElement.textContent).toContain('Le mot de passe est obligatoire.');
  });

  it('calls AuthService and navigates to the player area after a player login', () => {
    authService.login.mockReturnValue(of({ token: 'jwt', type: 'Bearer', roles: ['ROLE_JOUEUR'], hasPlayerProfile: true }));
    authService.isAdmin.mockReturnValue(false);
    authService.hasPlayerProfile.mockReturnValue(true);
    setFormValue({ username: 'joueur@example.test', password: 'secret' });

    submitForm();

    expect(authService.login).toHaveBeenCalledWith({
      username: 'joueur@example.test',
      password: 'secret'
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/espace-prive');
  });

  it('navigates to the admin area after an admin login', () => {
    authService.login.mockReturnValue(of({ token: 'jwt', type: 'Bearer', roles: ['ROLE_ADMIN_GLOBAL'], hasPlayerProfile: false }));
    authService.isAdmin.mockReturnValue(true);
    authService.hasPlayerProfile.mockReturnValue(false);
    setFormValue({ username: 'admin@example.test', password: 'secret' });

    submitForm();

    expect(authService.login).toHaveBeenCalledWith({
      username: 'admin@example.test',
      password: 'secret'
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/admin');
  });

  it('shows the authentication error message when login fails', () => {
    authService.login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401, error: { message: 'Unauthorized' } }))
    );
    setFormValue({ username: 'joueur@example.test', password: 'wrong' });

    submitForm();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Identifiant ou mot de passe invalide.');
  });

  function setFormValue(value: { username: string; password: string }): void {
    (fixture.componentInstance as unknown as {
      form: { setValue: (formValue: { username: string; password: string }) => void };
    }).form.setValue(value);
    fixture.detectChanges();
  }

  function submitForm(): void {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }
});
