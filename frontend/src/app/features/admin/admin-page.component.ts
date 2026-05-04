import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AdminInfoResponse } from '../../core/admin/admin.models';
import { AdminService } from '../../core/admin/admin.service';

interface AdminShortcutCard {
  title: string;
  description: string;
}

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-page.component.html',
  styleUrl: './admin-page.component.css'
})
export class AdminPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly adminService = inject(AdminService);

  protected readonly adminInfo = signal<AdminInfoResponse | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  protected readonly apiStatusMessage = computed(() => {
    const status = this.adminInfo()?.status;

    if (status === 'ok') {
      return 'API admin op\u00e9rationnelle';
    }

    return `Statut API admin : ${status ?? 'inconnu'}`;
  });

  protected readonly scopeLabel = computed(() => {
    if (this.authService.hasRole('ROLE_ADMIN_GLOBAL')) {
      return 'P\u00e9rim\u00e8tre : tous les sites';
    }

    if (this.authService.hasRole('ROLE_ADMIN_SITE')) {
      return 'P\u00e9rim\u00e8tre : site administr\u00e9';
    }

    return 'P\u00e9rim\u00e8tre : acc\u00e8s administrateur';
  });

  protected readonly shortcutCards: AdminShortcutCard[] = [
    { title: "Demandes d'inscription", description: 'Module à venir.' },
    { title: 'Joueurs', description: 'Module à venir.' },
    { title: 'Fermetures', description: 'Module à venir.' },
    { title: 'Statistiques', description: 'Module à venir.' },
    { title: 'Horaires', description: 'Module à venir.' }
  ];

  ngOnInit(): void {
    this.loadAdminInfo();
  }

  protected isApiOperational(): boolean {
    return this.adminInfo()?.status === 'ok';
  }

  private loadAdminInfo(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService
      .getInfo()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          this.adminInfo.set(response);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.getErrorMessage(error));
        }
      });
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Backend inaccessible.';
    }

    if (error.status === 401) {
      return 'Authentification requise.';
    }

    if (error.status === 403) {
      return 'Acc\u00e8s administrateur refus\u00e9.';
    }

    return "Impossible de charger l'espace d'administration.";
  }
}
