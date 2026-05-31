import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth/auth.service';
import { UiMessageService } from './ui-message.service';

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const uiMessageService = inject(UiMessageService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isLoginRequest(req.url)) {
        authService.logout();
        uiMessageService.show('Votre session a expiré. Veuillez vous reconnecter.');
        void router.navigateByUrl('/login');
      } else if (error.status === 403) {
        uiMessageService.show('Accès refusé.');
      } else if (error.status === 500) {
        uiMessageService.show('Une erreur serveur est survenue.');
      }

      return throwError(() => error);
    })
  );
};

function isLoginRequest(url: string): boolean {
  try {
    return new URL(url, 'http://localhost').pathname.endsWith('/auth/login');
  } catch {
    return url.endsWith('/auth/login');
  }
}
