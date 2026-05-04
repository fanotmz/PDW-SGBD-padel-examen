import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css'
})
export class HomePageComponent {
  private readonly authService = inject(AuthService);

  protected readonly isAdminView = computed(() => this.authService.isAdmin());
  protected readonly isPlayerView = computed(
    () => this.authService.isAuthenticated() && !this.authService.isAdmin() && this.authService.hasPlayerProfile()
  );
  protected readonly isVisitorView = computed(() => !this.authService.isAuthenticated());
}
