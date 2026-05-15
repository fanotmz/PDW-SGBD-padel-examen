import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminSiteConsultationResponse, AdminSiteScheduleResponse } from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-sites-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-sites-page.component.html',
  styleUrl: './admin-sites-page.component.css'
})
export class AdminSitesPageComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  protected readonly sites = signal<AdminSiteConsultationResponse[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly isSingleSiteView = computed(
    () => !this.isLoading() && !this.errorMessage() && this.sites().length === 1
  );
  protected readonly pageTitle = computed(() => (this.isSingleSiteView() ? 'Détail du site' : 'Détail des sites'));
  protected readonly pageIntro = computed(() =>
    this.isSingleSiteView()
      ? 'Consultez les informations du site : terrains, jours de fermeture, horaires et matchs.'
      : 'Consultez les informations des sites : terrains, jours de fermeture, horaires et matchs.'
  );

  private readonly dayLabels: Record<string, string> = {
    MONDAY: 'Lundi',
    TUESDAY: 'Mardi',
    WEDNESDAY: 'Mercredi',
    THURSDAY: 'Jeudi',
    FRIDAY: 'Vendredi',
    SATURDAY: 'Samedi',
    SUNDAY: 'Dimanche'
  };

  ngOnInit(): void {
    this.loadSites();
  }

  protected hasNoSites(): boolean {
    return !this.isLoading() && !this.errorMessage() && this.sites().length === 0;
  }

  protected trackSite(site: AdminSiteConsultationResponse): number {
    return site.id;
  }

  protected trackSchedule(schedule: AdminSiteScheduleResponse): number {
    return schedule.id;
  }

  protected getClosingDaysLabel(site: AdminSiteConsultationResponse): string {
    if (!site.joursFermeture || site.joursFermeture.length === 0) {
      return 'Aucun jour de fermeture récurrent.';
    }

    return site.joursFermeture
      .map((day) => this.dayLabels[day] ?? day)
      .join(', ');
  }

  protected formatTime(value: string): string {
    if (!value) {
      return 'Non renseigné';
    }

    return value.length >= 5 ? value.slice(0, 5) : value;
  }

  private loadSites(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService
      .getAdminSites()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (sites) => {
          this.sites.set(sites);
        },
        error: (error: HttpErrorResponse) => {
          this.sites.set([]);
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
      return 'Accès administrateur refusé.';
    }

    return 'Impossible de charger les sites.';
  }
}
