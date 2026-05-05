import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { PendingRegistrationItem } from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-registrations-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-registrations-page.component.html',
  styleUrl: './admin-registrations-page.component.css'
})
export class AdminRegistrationsPageComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  protected readonly registrations = signal<PendingRegistrationItem[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  protected readonly isEmpty = computed(
    () => !this.isLoading() && !this.errorMessage() && this.registrations().length === 0
  );

  ngOnInit(): void {
    this.loadPendingRegistrations();
  }

  protected getTypeLabel(type: PendingRegistrationItem['typeAbonnementDemande']): string {
    switch (type) {
      case 'GLOBAL':
        return 'Membre global';
      case 'SITE':
        return 'Membre d’un site';
      case 'LIBRE':
      default:
        return 'Membre libre';
    }
  }

  protected getStatusLabel(status: PendingRegistrationItem['status']): string {
    if (status === 'PENDING') {
      return 'En attente';
    }

    return status;
  }

  private loadPendingRegistrations(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService
      .getPendingRegistrations()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (items) => {
          this.registrations.set(items);
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

    return "Impossible de charger les demandes d'inscription.";
  }
}
