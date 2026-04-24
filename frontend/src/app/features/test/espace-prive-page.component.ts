import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';

type BackendTestStatus = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-espace-prive-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './espace-prive-page.component.html',
  styleUrl: './espace-prive-page.component.css'
})
export class EspacePrivePageComponent {
  private readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected backendTestStatus: BackendTestStatus = 'idle';
  protected backendTestMessage = '';

  protected testBackendCall(): void {
    this.backendTestStatus = 'loading';
    this.backendTestMessage = 'Appel backend en cours...';

    this.http
      .get(`${environment.apiBaseUrl}/ping`, { responseType: 'text' })
      .subscribe({
        next: () => {
          this.backendTestStatus = 'success';
          this.backendTestMessage = 'Succés: appel backend effectué.';
        },
        error: () => {
          this.backendTestStatus = 'error';
          this.backendTestMessage = 'Échec: appel backend impossible.';
        }
      });
  }

  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/');
  }
}
