import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin, of, switchMap } from 'rxjs';
import {
  AdminCaStatsResponse,
  AdminDettesStatsResponse,
  AdminInfoResponse,
  AdminMatchsStatsResponse
} from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';
import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { PageStateComponent } from '../../../shared/ui/page-state/page-state.component';

@Component({
  selector: 'app-admin-statistics-page',
  standalone: true,
  imports: [ReactiveFormsModule, PageHeaderComponent, PageStateComponent],
  templateUrl: './admin-statistics-page.component.html',
  styleUrl: './admin-statistics-page.component.css'
})
export class AdminStatisticsPageComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly sitesService = inject(SitesService);
  private readonly fb = inject(FormBuilder);

  protected readonly adminInfo = signal<AdminInfoResponse | null>(null);
  protected readonly sites = signal<SiteOption[]>([]);
  protected readonly selectedSiteId = signal<number | null>(null);
  protected readonly caStats = signal<AdminCaStatsResponse | null>(null);
  protected readonly matchsStats = signal<AdminMatchsStatsResponse | null>(null);
  protected readonly dettesStats = signal<AdminDettesStatsResponse | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly isLoadingStats = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly statsError = signal('');

  protected readonly periodForm = this.fb.group({
    from: this.fb.nonNullable.control(this.getFirstDayOfCurrentMonth(), [Validators.required]),
    to: this.fb.nonNullable.control(this.getToday(), [Validators.required])
  });

  protected readonly isAdminGlobal = computed(() => this.adminInfo()?.adminType === 'GLOBAL');
  protected readonly hasStats = computed(
    () => this.caStats() !== null && this.matchsStats() !== null && this.dettesStats() !== null
  );

  protected readonly selectedScopeLabel = computed(() => {
    const adminInfo = this.adminInfo();

    if (adminInfo?.adminType === 'SITE') {
      return adminInfo.siteNom ?? 'Site administr\u00e9';
    }

    const selectedSiteId = this.selectedSiteId();

    if (selectedSiteId == null) {
      return 'Tous les sites';
    }

    return this.sites().find((site) => site.id === selectedSiteId)?.nom ?? `Site ${selectedSiteId}`;
  });

  ngOnInit(): void {
    this.loadInitialData();
  }

  protected onScopeChange(value: string): void {
    if (value === 'global') {
      this.selectedSiteId.set(null);
      this.loadStats();
      return;
    }

    const siteId = Number(value);

    if (Number.isNaN(siteId)) {
      this.selectedSiteId.set(null);
      return;
    }

    this.selectedSiteId.set(siteId);
    this.loadStats();
  }

  protected refreshStats(): void {
    if (this.periodForm.invalid) {
      this.periodForm.markAllAsTouched();
      this.statsError.set('S\u00e9lectionnez une date de d\u00e9but et une date de fin.');
      return;
    }

    if (!this.isPeriodValid()) {
      this.statsError.set('La date de d\u00e9but doit \u00eatre ant\u00e9rieure ou \u00e9gale \u00e0 la date de fin.');
      return;
    }

    this.loadStats();
  }

  protected formatCurrency(value: number | null | undefined): string {
    return new Intl.NumberFormat('fr-BE', {
      style: 'currency',
      currency: 'EUR'
    }).format(Number(value ?? 0));
  }

  protected formatNumber(value: number | null | undefined): string {
    return new Intl.NumberFormat('fr-BE').format(Number(value ?? 0));
  }

  protected trackSite(site: SiteOption): number {
    return site.id;
  }

  private loadInitialData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.statsError.set('');

    this.adminService
      .getInfo()
      .pipe(
        switchMap((adminInfo) => {
          this.adminInfo.set(adminInfo);

          if (adminInfo.adminType === 'GLOBAL') {
            return this.sitesService.getSites();
          }

          if (adminInfo.siteId == null) {
            throw new Error('P\u00e9rim\u00e8tre administrateur introuvable.');
          }

          this.selectedSiteId.set(adminInfo.siteId);
          return of([] as SiteOption[]);
        }),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (sites) => {
          this.sites.set(sites);

          if (this.adminInfo()?.adminType === 'GLOBAL') {
            this.selectedSiteId.set(null);
          }

          this.loadStats();
        },
        error: (error: HttpErrorResponse | Error) => {
          this.clearStats();
          this.errorMessage.set(this.getErrorMessage(error, 'Impossible de charger la page statistiques.'));
        }
      });
  }

  private loadStats(): void {
    const from = this.periodForm.controls.from.value;
    const to = this.periodForm.controls.to.value;

    if (!from || !to) {
      this.statsError.set('S\u00e9lectionnez une date de d\u00e9but et une date de fin.');
      return;
    }

    if (!this.isPeriodValid()) {
      this.statsError.set('La date de d\u00e9but doit \u00eatre ant\u00e9rieure ou \u00e9gale \u00e0 la date de fin.');
      return;
    }

    const adminInfo = this.adminInfo();

    if (!adminInfo) {
      this.statsError.set('Informations administrateur indisponibles.');
      return;
    }

    const siteId = this.getCurrentSiteId();

    if (adminInfo.adminType === 'SITE' && siteId == null) {
      this.statsError.set('P\u00e9rim\u00e8tre administrateur introuvable.');
      return;
    }

    const request = adminInfo.adminType === 'GLOBAL' && siteId == null
      ? forkJoin({
          ca: this.adminService.getGlobalCaStats(from, to),
          matchs: this.adminService.getGlobalMatchsStats(from, to),
          dettes: this.adminService.getGlobalDettesStats()
        })
      : forkJoin({
          ca: this.adminService.getSiteCaStats(siteId as number, from, to),
          matchs: this.adminService.getSiteMatchsStats(siteId as number, from, to),
          dettes: this.adminService.getSiteDettesStats(siteId as number)
        });

    this.isLoadingStats.set(true);
    this.statsError.set('');

    request
      .pipe(finalize(() => this.isLoadingStats.set(false)))
      .subscribe({
        next: ({ ca, matchs, dettes }) => {
          this.caStats.set(ca);
          this.matchsStats.set(matchs);
          this.dettesStats.set(dettes);
        },
        error: (error: HttpErrorResponse) => {
          this.clearStats();
          this.statsError.set(this.getErrorMessage(error, 'Impossible de charger les statistiques.'));
        }
      });
  }

  private getCurrentSiteId(): number | null {
    const adminInfo = this.adminInfo();

    if (adminInfo?.adminType === 'SITE') {
      return adminInfo.siteId;
    }

    return this.selectedSiteId();
  }

  private clearStats(): void {
    this.caStats.set(null);
    this.matchsStats.set(null);
    this.dettesStats.set(null);
  }

  private isPeriodValid(): boolean {
    const from = this.periodForm.controls.from.value;
    const to = this.periodForm.controls.to.value;

    return Boolean(from && to && from <= to);
  }

  private getFirstDayOfCurrentMonth(): string {
    const now = new Date();
    return this.toDateInputValue(new Date(now.getFullYear(), now.getMonth(), 1));
  }

  private getToday(): string {
    return this.toDateInputValue(new Date());
  }

  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  private getErrorMessage(error: HttpErrorResponse | Error, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'Backend inaccessible.';
      }

      const backendMessage = this.getBackendMessage(error);

      if (backendMessage) {
        return backendMessage;
      }

      if (error.status === 401) {
        return 'Authentification requise.';
      }

      if (error.status === 403) {
        return 'Acc\u00e8s administrateur refus\u00e9.';
      }

      if (error.status === 404) {
        return 'Site introuvable.';
      }

      return fallback;
    }

    return error.message || fallback;
  }

  private getBackendMessage(error: HttpErrorResponse): string {
    const body = error.error as Partial<ApiErrorResponse> | string | null;

    if (typeof body === 'string') {
      return body;
    }

    if (body?.message) {
      return body.message;
    }

    return '';
  }
}
