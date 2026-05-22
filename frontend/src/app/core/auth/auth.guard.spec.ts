import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  const authServiceMock = {
    isAuthenticated: vi.fn()
  };

  function runGuard(url: string): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot)
    ) as boolean | UrlTree;
  }

  beforeEach(() => {
    authServiceMock.isAuthenticated.mockReset();

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock }
      ]
    });
  });

  it('allows access when the user is authenticated', () => {
    authServiceMock.isAuthenticated.mockReturnValue(true);

    expect(runGuard('/matchs')).toBe(true);
    expect(authServiceMock.isAuthenticated).toHaveBeenCalledOnce();
  });

  it('redirects to login with returnUrl when the user is not authenticated', () => {
    authServiceMock.isAuthenticated.mockReturnValue(false);

    const result = runGuard('/matchs/12');
    const router = TestBed.inject(Router);

    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).queryParams['returnUrl']).toBe('/matchs/12');
    expect(router.serializeUrl(result as UrlTree).startsWith('/login')).toBe(true);
  });
});
