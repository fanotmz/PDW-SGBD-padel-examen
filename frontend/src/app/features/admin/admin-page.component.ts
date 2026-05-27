import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AdminInfoResponse, PendingRegistrationItem } from '../../core/admin/admin.models';
import { AdminService } from '../../core/admin/admin.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PageStateComponent } from '../../shared/ui/page-state/page-state.component';
import { UI_MESSAGES } from '../../shared/ui/ui-messages';

interface AdminShortcutCard {
  title: string;
  description: string;
  icon: string;
  route?: string;
}

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [
    RouterLink,
    MatCardModule,
    MatIconModule,
    PageHeaderComponent,
    PageStateComponent
  ],
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
  protected readonly uiMessages = UI_MESSAGES;
  protected readonly canViewRegistrationRequests = computed(() => this.authService.hasRole('ROLE_ADMIN_GLOBAL'));
  protected readonly isSiteScopedAdmin = computed(
    () => this.adminInfo()?.adminType === 'SITE' || this.authService.hasRole('ROLE_ADMIN_SITE')
  );

  protected readonly apiStatusMessage = computed(() => {
    const status = this.adminInfo()?.status;

    if (status === 'ok') {
      return 'API admin opérationnelle';
    }

    return `Statut API admin : ${status ?? 'inconnu'}`;
  });

  protected readonly scopeLabel = computed(() => {
    const adminInfo = this.adminInfo();

    if (adminInfo?.adminType === 'GLOBAL') {
      return 'Périmètre : tous les sites';
    }

    if (adminInfo?.adminType === 'SITE' && adminInfo.siteNom) {
      return `Périmètre : ${adminInfo.siteNom}`;
    }

    if (this.authService.hasRole('ROLE_ADMIN_SITE')) {
      return 'Périmètre : site administré';
    }

    return 'Périmètre : accès administrateur';
  });

  protected readonly adminAccessMessage = computed(() => {
    const adminInfo = this.adminInfo();

    if (adminInfo?.adminType === 'SITE' && adminInfo.siteNom) {
      return `Accès administrateur actif pour le site ${adminInfo.siteNom}.`;
    }

    if (this.authService.hasRole('ROLE_ADMIN_SITE')) {
      return 'Accès administrateur actif pour le site rattaché à votre compte.';
    }

    return 'Accès administrateur actif pour votre périmètre.';
  });

  protected readonly registrationSummary = computed(() => {
    if (!this.canViewRegistrationRequests()) {
      return '';
    }

    if (this.isLoadingRegistrations()) {
      return 'Chargement des demandes...';
    }

    if (this.registrationsError()) {
      return 'Demandes non chargées';
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
        icon: 'how_to_reg',
        route: '/admin/inscriptions'
      });
    }

    cards.push(
      { title: 'Joueurs', description: 'Consultez les joueurs enregistrés dans votre périmètre.', icon: 'groups', route: '/admin/joueurs' },
      {
        title: this.isSiteScopedAdmin() ? 'Détail du site' : 'Détail des sites',
        description: this.isSiteScopedAdmin()
          ? 'Consultez les informations du site : terrains, jours de fermeture, horaires et matchs planifiés.'
          : 'Consultez les sites, leurs terrains, leurs jours de fermeture, leurs horaires et leurs matchs planifiés.',
        icon: 'location_on',
        route: '/admin/sites'
      },
      { title: 'Fermetures', description: 'Consultez les fermetures globales et les fermetures de site.', icon: 'event_busy', route: '/admin/fermetures' },
      { title: 'Horaires', description: 'Consultez les horaires configurés par site.', icon: 'schedule', route: '/admin/horaires' },
      { title: 'Statistiques', description: "Consultez le chiffre d'affaires, les matchs et les dettes.", icon: 'bar_chart', route: '/admin/statistiques' }
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
      return 'Accès administrateur refusé.';
    }

    return "Impossible de charger l'espace d'administration.";
  }
}
