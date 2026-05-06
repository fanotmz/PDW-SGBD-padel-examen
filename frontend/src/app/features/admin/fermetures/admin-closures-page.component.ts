import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin, of, switchMap } from 'rxjs';
import {
  AdminGlobalClosureResponse,
  AdminInfoResponse,
  AdminSiteClosureResponse
} from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';

@Component({
  selector: 'app-admin-closures-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-closures-page.component.html',
  styleUrl: './admin-closures-page.component.css'
})
export class AdminClosuresPageComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly sitesService = inject(SitesService);

  protected readonly adminInfo = signal<AdminInfoResponse | null>(null);
  protected readonly globalClosures = signal<AdminGlobalClosureResponse[]>([]);
  protected readonly siteClosures = signal<AdminSiteClosureResponse[]>([]);
  protected readonly sites = signal<SiteOption[]>([]);
  protected readonly selectedSiteId = signal<number | null>(null);

  protected readonly isLoading = signal(true);
  protected readonly isLoadingSiteClosures = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly globalClosuresError = signal('');
  protected readonly siteClosuresError = signal('');

  protected readonly isAdminGlobal = computed(() => this.adminInfo()?.adminType === 'GLOBAL');
  protected readonly hasNoGlobalClosures = computed(() => !this.globalClosuresError() && this.globalClosures().length === 0);
  protected readonly hasNoSiteClosures = computed(
    () => !this.siteClosuresError() && !this.isLoadingSiteClosures() && this.siteClosures().length === 0
  );

  protected readonly selectedSiteName = computed(() => {
    const selectedSiteId = this.selectedSiteId();

    if (selectedSiteId == null) {
      return '';
    }

    return this.sites().find((site) => site.id === selectedSiteId)?.nom ?? `Site ${selectedSiteId}`;
  });

  protected readonly siteScopeLabel = computed(() => {
    const adminInfo = this.adminInfo();

    if (adminInfo?.adminType === 'SITE') {
      return adminInfo.siteNom ?? 'Site administré';
    }

    return this.selectedSiteName();
  });

  ngOnInit(): void {
    this.loadClosures();
  }

  protected onSiteChange(value: string): void {
    const siteId = Number(value);

    if (Number.isNaN(siteId)) {
      this.selectedSiteId.set(null);
      this.siteClosures.set([]);
      return;
    }

    this.selectedSiteId.set(siteId);
    this.loadSiteClosures(siteId);
  }

  protected getGlobalClosureTrack(closure: AdminGlobalClosureResponse): number {
    return closure.id;
  }

  protected getSiteClosureTrack(closure: AdminSiteClosureResponse): number {
    return closure.id;
  }

  protected getMotifLabel(motif: string | null): string {
    const trimmedMotif = motif?.trim();
    return trimmedMotif ? trimmedMotif : 'Aucun motif renseigné';
  }

  protected getSiteClosureTypeLabel(closure: AdminSiteClosureResponse): string {
    return closure.date ? 'Date unique' : 'Période';
  }

  protected getSiteClosureDateLabel(closure: AdminSiteClosureResponse): string {
    if (closure.date) {
      return this.formatDate(closure.date);
    }

    if (closure.dateDebut && closure.dateFin) {
      return `${this.formatDate(closure.dateDebut)} au ${this.formatDate(closure.dateFin)}`;
    }

    return 'Dates indisponibles';
  }

  protected formatDate(value: string): string {
    const date = new Date(`${value}T00:00:00`);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('fr-BE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(date);
  }

  private loadClosures(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.globalClosuresError.set('');
    this.siteClosuresError.set('');

    this.adminService
      .getInfo()
      .pipe(
        switchMap((adminInfo) => {
          this.adminInfo.set(adminInfo);

          if (adminInfo.adminType === 'GLOBAL') {
            return forkJoin({
              globalClosures: this.adminService.getGlobalClosures(),
              sites: this.sitesService.getSites()
            });
          }

          return forkJoin({
            globalClosures: this.adminService.getGlobalClosures(),
            sites: of([] as SiteOption[])
          });
        }),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: ({ globalClosures, sites }) => {
          this.globalClosures.set(globalClosures);
          this.sites.set(sites);
          this.loadInitialSiteClosures();
        },
        error: (error: HttpErrorResponse) => {
          this.globalClosures.set([]);
          this.siteClosures.set([]);
          this.errorMessage.set(this.getPageErrorMessage(error));
        }
      });
  }

  private loadInitialSiteClosures(): void {
    const adminInfo = this.adminInfo();

    if (adminInfo?.adminType === 'GLOBAL') {
      const firstSite = this.sites()[0];

      if (!firstSite) {
        this.selectedSiteId.set(null);
        this.siteClosures.set([]);
        return;
      }

      this.selectedSiteId.set(firstSite.id);
      this.loadSiteClosures(firstSite.id);
      return;
    }

    if (adminInfo?.adminType === 'SITE' && adminInfo.siteId != null) {
      this.selectedSiteId.set(adminInfo.siteId);
      this.loadSiteClosures(adminInfo.siteId);
      return;
    }

    this.siteClosuresError.set('Périmètre administrateur introuvable.');
  }

  private loadSiteClosures(siteId: number): void {
    this.isLoadingSiteClosures.set(true);
    this.siteClosuresError.set('');

    this.adminService
      .getSiteClosures(siteId)
      .pipe(finalize(() => this.isLoadingSiteClosures.set(false)))
      .subscribe({
        next: (closures) => {
          this.siteClosures.set(closures);
        },
        error: (error: HttpErrorResponse) => {
          this.siteClosures.set([]);
          this.siteClosuresError.set(this.getSiteClosuresErrorMessage(error));
        }
      });
  }

  private getPageErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Backend inaccessible.';
    }

    if (error.status === 401) {
      return 'Authentification requise.';
    }

    if (error.status === 403) {
      return 'Accès administrateur refusé.';
    }

    return 'Impossible de charger les fermetures.';
  }

  private getSiteClosuresErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Backend inaccessible.';
    }

    if (error.status === 401) {
      return 'Authentification requise.';
    }

    if (error.status === 403) {
      return 'Accès refusé pour ce site.';
    }

    if (error.status === 404) {
      return 'Site introuvable.';
    }

    return 'Impossible de charger les fermetures de ce site.';
  }
}
