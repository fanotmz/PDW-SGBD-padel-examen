import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AdminInfoResponse, PendingRegistrationItem } from '../../core/admin/admin.models';
import { AdminService } from '../../core/admin/admin.service';

interface AdminShortcutCard {
  title: string;
  description: string;
  route?: string;
}

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-page.component.html',
  styleUrl: './admin-page.component.css'
})
export class AdminPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly adminService = inject(AdminService);

  protected readonly adminInfo = signal<AdminInfoResponse | null>(null);
  protected readonly pendingRegistrations = signal<PendingRegistrationItem[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly isLoadingRegistrations = signal(false);
  protected readonly registrationsError = signal(false);
  protected readonly canViewRegistrationRequests = computed(() => this.authService.hasRole('ROLE_ADMIN_GLOBAL'));

  protected readonly apiStatusMessage = computed(() => {
    const status = this.adminInfo()?.status;

    if (status === 'ok') {
      return 'API admin op\u00e9rationnelle';
    }

    return `Statut API admin : ${status ?? 'inconnu'}`;
  });

  protected readonly scopeLabel = computed(() => {
    const adminInfo = this.adminInfo();

    if (adminInfo?.adminType === 'GLOBAL') {
      return 'P\u00e9rim\u00e8tre : tous les sites';
    }

    if (adminInfo?.adminType === 'SITE' && adminInfo.siteNom) {
      return `P\u00e9rim\u00e8tre : ${adminInfo.siteNom}`;
    }

    if (this.authService.hasRole('ROLE_ADMIN_SITE')) {
      return 'P\u00e9rim\u00e8tre : site administr\u00e9';
    }

    return 'P\u00e9rim\u00e8tre : acc\u00e8s administrateur';
  });

  protected readonly registrationSummary = computed(() => {
    if (!this.canViewRegistrationRequests()) {
      return '';
    }

    if (this.isLoadingRegistrations()) {
      return 'Chargement des demandes...';
    }

    if (this.registrationsError()) {
      return 'Demandes non charg\u00e9es';
    }

    const count = this.pendingRegistrations().length;

    if (count === 0) {
      return 'Aucune demande en attente de validation';
    }

    if (count === 1) {
      return '1 demande en attente de validation';
    }

    return `${count} demandes en attente de validation`;
  });

  protected readonly shortcutCards = computed<AdminShortcutCard[]>(() => {
    const cards: AdminShortcutCard[] = [];

    if (this.canViewRegistrationRequests()) {
      cards.push({
        title: "Demandes d'inscription",
        description: 'Consultez les comptes en attente de validation.',
        route: '/admin/inscriptions'
      });
    }

    cards.push(
      { title: 'Joueurs', description: 'Consultez les joueurs enregistr\u00e9s dans votre p\u00e9rim\u00e8tre.', route: '/admin/joueurs' },
      { title: 'Fermetures', description: 'Module \u00e0 venir.' },
      { title: 'Statistiques', description: 'Module \u00e0 venir.' },
      { title: 'Horaires', description: 'Module \u00e0 venir.' }
    );

    return cards;
  });

  ngOnInit(): void {
    this.loadAdminInfo();

    if (this.canViewRegistrationRequests()) {
      this.loadPendingRegistrations();
    }
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

  private loadPendingRegistrations(): void {
    this.isLoadingRegistrations.set(true);
    this.registrationsError.set(false);

    this.adminService
      .getPendingRegistrations()
      .pipe(finalize(() => this.isLoadingRegistrations.set(false)))
      .subscribe({
        next: (registrations) => {
          this.pendingRegistrations.set(registrations);
        },
        error: () => {
          this.pendingRegistrations.set([]);
          this.registrationsError.set(true);
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
