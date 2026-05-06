import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin, Observable, of, switchMap } from 'rxjs';
import {
  AdminGlobalClosureResponse,
  AdminInfoResponse,
  AdminSiteClosureResponse,
  CreateGlobalClosureRequest,
  CreateSiteDateClosureRequest,
  CreateSitePeriodClosureRequest
} from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';
import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';

type SiteClosureMode = 'date' | 'period';
type PendingClosureAction = 'global' | 'site-date' | 'site-period' | null;

@Component({
  selector: 'app-admin-closures-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-closures-page.component.html',
  styleUrl: './admin-closures-page.component.css'
})
export class AdminClosuresPageComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly sitesService = inject(SitesService);
  private readonly fb = inject(FormBuilder);

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
  protected readonly successMessage = signal('');
  protected readonly globalCreateError = signal('');
  protected readonly siteCreateError = signal('');
  protected readonly pendingAction = signal<PendingClosureAction>(null);
  protected readonly isSubmittingGlobalClosure = signal(false);
  protected readonly isSubmittingSiteClosure = signal(false);

  protected readonly siteClosureModes: Array<{ value: SiteClosureMode; label: string }> = [
    { value: 'date', label: 'Date unique' },
    { value: 'period', label: 'P\u00e9riode' }
  ];

  protected readonly globalClosureForm = this.fb.group({
    date: this.fb.nonNullable.control('', [Validators.required]),
    motif: this.fb.control<string | null>(null)
  });

  protected readonly siteClosureForm = this.fb.group({
    mode: this.fb.nonNullable.control<SiteClosureMode>('date', [Validators.required]),
    date: this.fb.control<string | null>(null, [Validators.required]),
    dateDebut: this.fb.control<string | null>(null),
    dateFin: this.fb.control<string | null>(null),
    motif: this.fb.control<string | null>(null)
  });

  protected readonly isAdminGlobal = computed(() => this.adminInfo()?.adminType === 'GLOBAL');
  protected readonly hasNoGlobalClosures = computed(() => !this.globalClosuresError() && this.globalClosures().length === 0);
  protected readonly hasNoSiteClosures = computed(
    () => !this.siteClosuresError() && !this.isLoadingSiteClosures() && this.siteClosures().length === 0
  );
  protected readonly canCreateSiteClosure = computed(() => this.getCurrentSiteId() != null);

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
    this.applySiteModeValidators(this.siteClosureForm.controls.mode.value);

    this.siteClosureForm.controls.mode.valueChanges.subscribe((mode) => {
      this.pendingAction.set(null);
      this.siteCreateError.set('');
      this.applySiteModeValidators(mode);
    });

    this.loadClosures();
  }

  protected onSiteChange(value: string): void {
    const siteId = Number(value);

    if (Number.isNaN(siteId)) {
      this.selectedSiteId.set(null);
      this.siteClosures.set([]);
      this.pendingAction.set(null);
      return;
    }

    this.selectedSiteId.set(siteId);
    this.pendingAction.set(null);
    this.siteCreateError.set('');
    this.loadSiteClosures(siteId);
  }

  protected requestGlobalClosureCreation(): void {
    this.successMessage.set('');
    this.globalCreateError.set('');
    this.pendingAction.set(null);

    if (this.globalClosureForm.invalid) {
      this.globalClosureForm.markAllAsTouched();
      this.globalCreateError.set('La date est obligatoire.');
      return;
    }

    this.pendingAction.set('global');
  }

  protected requestSiteClosureCreation(): void {
    this.successMessage.set('');
    this.siteCreateError.set('');
    this.pendingAction.set(null);
    this.applySiteModeValidators(this.siteClosureForm.controls.mode.value);

    if (this.getCurrentSiteId() == null) {
      this.siteCreateError.set('Aucun site disponible pour cette fermeture.');
      return;
    }

    if (this.siteClosureForm.invalid) {
      this.siteClosureForm.markAllAsTouched();
      this.siteCreateError.set(this.getSiteFormValidationMessage());
      return;
    }

    this.pendingAction.set(this.siteClosureForm.controls.mode.value === 'date' ? 'site-date' : 'site-period');
  }

  protected cancelPendingAction(): void {
    this.pendingAction.set(null);
  }

  protected confirmPendingAction(): void {
    const action = this.pendingAction();

    if (action === 'global') {
      this.confirmGlobalClosureCreation();
      return;
    }

    if (action === 'site-date') {
      this.confirmSiteDateClosureCreation();
      return;
    }

    if (action === 'site-period') {
      this.confirmSitePeriodClosureCreation();
    }
  }

  protected isPendingAction(action: PendingClosureAction): boolean {
    return this.pendingAction() === action;
  }

  protected showGlobalDateError(): boolean {
    const control = this.globalClosureForm.controls.date;
    return control.invalid && (control.dirty || control.touched);
  }

  protected showSiteDateError(): boolean {
    const control = this.siteClosureForm.controls.date;
    return this.siteClosureForm.controls.mode.value === 'date' && control.invalid && (control.dirty || control.touched);
  }

  protected showSiteDateDebutError(): boolean {
    const control = this.siteClosureForm.controls.dateDebut;
    return this.siteClosureForm.controls.mode.value === 'period' && control.invalid && (control.dirty || control.touched);
  }

  protected showSiteDateFinError(): boolean {
    const control = this.siteClosureForm.controls.dateFin;
    return this.siteClosureForm.controls.mode.value === 'period' && control.invalid && (control.dirty || control.touched);
  }

  protected isSiteClosureMode(mode: SiteClosureMode): boolean {
    return this.siteClosureForm.controls.mode.value === mode;
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

  private confirmGlobalClosureCreation(): void {
    if (this.globalClosureForm.invalid) {
      this.globalClosureForm.markAllAsTouched();
      this.globalCreateError.set('La date est obligatoire.');
      this.pendingAction.set(null);
      return;
    }

    const rawValue = this.globalClosureForm.getRawValue();
    const payload: CreateGlobalClosureRequest = {
      date: rawValue.date,
      motif: this.normalizeMotif(rawValue.motif)
    };

    this.isSubmittingGlobalClosure.set(true);
    this.globalCreateError.set('');

    this.adminService
      .createGlobalClosure(payload)
      .pipe(finalize(() => this.isSubmittingGlobalClosure.set(false)))
      .subscribe({
        next: () => {
          this.pendingAction.set(null);
          this.globalClosureForm.reset({ date: '', motif: null });
          this.successMessage.set('Fermeture globale cr\u00e9\u00e9e.');
          this.loadGlobalClosures();
        },
        error: (error: HttpErrorResponse) => {
          this.globalCreateError.set(this.getCreateErrorMessage(error, 'Impossible de cr\u00e9er la fermeture globale.'));
        }
      });
  }

  private confirmSiteDateClosureCreation(): void {
    const siteId = this.getCurrentSiteId();
    const date = this.siteClosureForm.controls.date.value;

    if (siteId == null) {
      this.siteCreateError.set('Aucun site disponible pour cette fermeture.');
      this.pendingAction.set(null);
      return;
    }

    if (!date) {
      this.siteClosureForm.controls.date.markAsTouched();
      this.siteCreateError.set('La date est obligatoire.');
      this.pendingAction.set(null);
      return;
    }

    const payload: CreateSiteDateClosureRequest = {
      date,
      motif: this.normalizeMotif(this.siteClosureForm.controls.motif.value)
    };

    this.submitSiteClosure(siteId, this.adminService.createSiteDateClosure(siteId, payload));
  }

  private confirmSitePeriodClosureCreation(): void {
    const siteId = this.getCurrentSiteId();
    const dateDebut = this.siteClosureForm.controls.dateDebut.value;
    const dateFin = this.siteClosureForm.controls.dateFin.value;

    if (siteId == null) {
      this.siteCreateError.set('Aucun site disponible pour cette fermeture.');
      this.pendingAction.set(null);
      return;
    }

    if (!dateDebut || !dateFin) {
      this.siteClosureForm.markAllAsTouched();
      this.siteCreateError.set('La p\u00e9riode doit contenir une date de d\u00e9but et une date de fin.');
      this.pendingAction.set(null);
      return;
    }

    const payload: CreateSitePeriodClosureRequest = {
      dateDebut,
      dateFin,
      motif: this.normalizeMotif(this.siteClosureForm.controls.motif.value)
    };

    this.submitSiteClosure(siteId, this.adminService.createSitePeriodClosure(siteId, payload));
  }

  private submitSiteClosure(siteId: number, request$: Observable<unknown>): void {
    this.isSubmittingSiteClosure.set(true);
    this.siteCreateError.set('');

    request$
      .pipe(finalize(() => this.isSubmittingSiteClosure.set(false)))
      .subscribe({
        next: () => {
          this.pendingAction.set(null);
          this.siteClosureForm.reset({
            mode: 'date',
            date: null,
            dateDebut: null,
            dateFin: null,
            motif: null
          });
          this.applySiteModeValidators('date');
          this.successMessage.set('Fermeture du site cr\u00e9\u00e9e.');
          this.loadSiteClosures(siteId);
        },
        error: (error: HttpErrorResponse) => {
          this.siteCreateError.set(this.getCreateErrorMessage(error, 'Impossible de cr\u00e9er la fermeture du site.'));
        }
      });
  }

  private loadGlobalClosures(): void {
    this.globalClosuresError.set('');

    this.adminService
      .getGlobalClosures()
      .subscribe({
        next: (closures) => {
          this.globalClosures.set(closures);
        },
        error: (error: HttpErrorResponse) => {
          this.globalClosures.set([]);
          this.globalClosuresError.set(this.getPageErrorMessage(error));
        }
      });
  }

  private applySiteModeValidators(mode: SiteClosureMode): void {
    const dateControl = this.siteClosureForm.controls.date;
    const dateDebutControl = this.siteClosureForm.controls.dateDebut;
    const dateFinControl = this.siteClosureForm.controls.dateFin;

    if (mode === 'date') {
      dateControl.setValidators([Validators.required]);
      dateDebutControl.clearValidators();
      dateFinControl.clearValidators();
    } else {
      dateControl.clearValidators();
      dateDebutControl.setValidators([Validators.required]);
      dateFinControl.setValidators([Validators.required]);
    }

    dateControl.updateValueAndValidity({ emitEvent: false });
    dateDebutControl.updateValueAndValidity({ emitEvent: false });
    dateFinControl.updateValueAndValidity({ emitEvent: false });
  }

  private getCurrentSiteId(): number | null {
    const adminInfo = this.adminInfo();

    if (adminInfo?.adminType === 'SITE') {
      return adminInfo.siteId;
    }

    return this.selectedSiteId();
  }

  private normalizeMotif(value: string | null | undefined): string | null {
    const trimmedValue = value?.trim();
    return trimmedValue ? trimmedValue : null;
  }

  private getSiteFormValidationMessage(): string {
    if (this.siteClosureForm.controls.mode.value === 'date') {
      return 'La date est obligatoire.';
    }

    return 'La p\u00e9riode doit contenir une date de d\u00e9but et une date de fin.';
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

  private getCreateErrorMessage(error: HttpErrorResponse, fallbackMessage: string): string {
    const apiError = this.normalizeApiErrorBody(error.error);

    if (error.status === 0) {
      return 'Backend inaccessible.';
    }

    if (error.status === 401) {
      return 'Authentification requise.';
    }

    if (error.status === 403) {
      return 'Acc\u00e8s refus\u00e9.';
    }

    if (error.status === 404) {
      return apiError.message || 'Site introuvable.';
    }

    if (apiError.details) {
      return Object.values(apiError.details).join(' ');
    }

    return apiError.message || fallbackMessage;
  }

  private normalizeApiErrorBody(raw: unknown): Partial<ApiErrorResponse> {
    if (typeof raw === 'string') {
      try {
        return JSON.parse(raw) as Partial<ApiErrorResponse>;
      } catch {
        return { message: raw };
      }
    }

    if (raw && typeof raw === 'object') {
      return raw as Partial<ApiErrorResponse>;
    }

    return {};
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
