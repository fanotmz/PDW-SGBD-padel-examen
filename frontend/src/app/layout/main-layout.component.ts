import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatIconModule,
    MatMenuModule
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css'
})
export class MainLayoutComponent {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly adminSitesLabel = computed(() =>
    this.authService.hasRole('ROLE_ADMIN_GLOBAL') ? 'Sites' : 'Site'
  );

  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/');
  }
}
