import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatCardModule, MatIconModule],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css'
})
export class HomePageComponent {
  private readonly authService = inject(AuthService);

  protected readonly isAdminView = computed(() => this.authService.isAdmin());
  protected readonly hasPlayerProfile = computed(() => this.authService.hasPlayerProfile());
  protected readonly isPlayerView = computed(
    () => this.authService.isAuthenticated() && !this.authService.isAdmin() && this.authService.hasPlayerProfile()
  );
  protected readonly isVisitorView = computed(() => !this.authService.isAuthenticated());
  protected readonly adminSitesLabel = computed(() =>
    this.authService.hasRole('ROLE_ADMIN_GLOBAL') ? 'Sites' : 'Site'
  );
  protected readonly adminSitesDescription = computed(() =>
    this.authService.hasRole('ROLE_ADMIN_GLOBAL')
      ? 'Consultez les sites, terrains, horaires et fermetures.'
      : 'Consultez le site, ses terrains, ses horaires et ses fermetures.'
  );
}
