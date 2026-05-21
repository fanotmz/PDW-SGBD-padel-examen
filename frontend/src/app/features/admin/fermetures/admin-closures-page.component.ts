import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin, Observable, of, switchMap } from 'rxjs';
import {
  AdminGlobalClosureResponse,
  AdminInfoResponse,
  AdminSiteClosureResponse,
  CreateGlobalClosureRequest,
  CreateSiteDateClosureRequest,
  CreateSitePeriodClosureRequest,
  UpdateSiteDateClosureRequest,
  UpdateSitePeriodClosureRequest
} from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';
import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';

type SiteClosureMode = 'date' | 'period';
type PendingClosureAction = 'global' | 'site-date' | 'site-period' | null;
type PendingDeleteAction =
  | { type: 'global'; id: number }
  | { type: 'site'; id: number }
  | null;
type PendingEditAction = { id: number; mode: SiteClosureMode } | null;

@Component({
  selector: 'app-admin-closures-page',
  standalone: true,
  imports: [ReactiveFormsModule, PageHeaderComponent],
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
  protected readonly globalDeleteError = signal('');
  protected readonly siteDeleteError = signal('');
  protected readonly siteUpdateError = signal('');
  protected readonly pendingAction = signal<PendingClosureAction>(null);
  protected readonly pendingDeleteAction = signal<PendingDeleteAction>(null);
  protected readonly pendingEditAction = signal<PendingEditAction>(null);
  protected readonly activeEditClosureId = signal<number | null>(null);
  protected readonly isSubmittingGlobalClosure = signal(false);
  protected readonly isSubmittingSiteClosure = signal(false);
  protected readonly deletingGlobalClosureId = signal<number | null>(null);
  protected readonly deletingSiteClosureId = signal<number | null>(null);
  protected readonly updatingSiteClosureId = signal<number | null>(null);

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

  protected readonly editSiteClosureForm = this.fb.group({
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
  protected readonly canDeleteSiteClosure = computed(() => this.getCurrentSiteId() != null);

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

    this.editSiteClosureForm.controls.mode.valueChanges.subscribe((mode) => {
      this.pendingEditAction.set(null);
      this.siteUpdateError.set('');
      this.applyEditModeValidators(mode);
    });

    this.loadClosures();
  }

  protected onSiteChange(value: string): void {
    const siteId = Number(value);

    if (Number.isNaN(siteId)) {
      this.selectedSiteId.set(null);
      this.siteClosures.set([]);
      this.pendingAction.set(null);
      this.pendingDeleteAction.set(null);
      this.cancelSiteClosureEdit();
      return;
    }

    this.selectedSiteId.set(siteId);
    this.pendingAction.set(null);
    this.pendingDeleteAction.set(null);
    this.cancelSiteClosureEdit();
    this.siteCreateError.set('');
    this.siteDeleteError.set('');
    this.siteUpdateError.set('');
    this.loadSiteClosures(siteId);
  }

  protected requestGlobalClosureCreation(): void {
    this.successMessage.set('');
    this.globalCreateError.set('');
    this.pendingAction.set(null);
    this.pendingDeleteAction.set(null);

    if (this.globalClosureForm.invalid) {
      this.globalClosureForm.markAllAsTouched();
      return;
    }

    this.pendingAction.set('global');
  }

  protected requestSiteClosureCreation(): void {
    this.successMessage.set('');
    this.siteCreateError.set('');
    this.pendingAction.set(null);
    this.pendingDeleteAction.set(null);
    this.cancelSiteClosureEdit();
    this.applySiteModeValidators(this.siteClosureForm.controls.mode.value);

    if (this.getCurrentSiteId() == null) {
      this.siteCreateError.set('Aucun site disponible pour cette fermeture.');
      return;
    }

    if (this.siteClosureForm.invalid) {
      this.siteClosureForm.markAllAsTouched();
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

  protected requestGlobalClosureDeletion(closure: AdminGlobalClosureResponse): void {
    this.successMessage.set('');
    this.globalDeleteError.set('');
    this.pendingAction.set(null);
    this.cancelSiteClosureEdit();
    this.pendingDeleteAction.set({ type: 'global', id: closure.id });
  }

  protected requestSiteClosureDeletion(closure: AdminSiteClosureResponse): void {
    this.successMessage.set('');
    this.siteDeleteError.set('');
    this.pendingAction.set(null);
    this.cancelSiteClosureEdit();
    this.pendingDeleteAction.set({ type: 'site', id: closure.id });
  }

  protected cancelDeleteConfirmation(): void {
    this.pendingDeleteAction.set(null);
  }

  protected confirmDeleteAction(): void {
    const action = this.pendingDeleteAction();

    if (action?.type === 'global') {
      this.deleteGlobalClosure(action.id);
      return;
    }

    if (action?.type === 'site') {
      this.deleteSiteClosure(action.id);
    }
  }

  protected isPendingGlobalDelete(id: number): boolean {
    const action = this.pendingDeleteAction();
    return action?.type === 'global' && action.id === id;
  }

  protected isPendingSiteDelete(id: number): boolean {
    const action = this.pendingDeleteAction();
    return action?.type === 'site' && action.id === id;
  }

  protected canDeleteGlobalClosure(): boolean {
    return this.isAdminGlobal();
  }

  protected openSiteClosureEdit(closure: AdminSiteClosureResponse): void {
    this.successMessage.set('');
    this.siteUpdateError.set('');
    this.pendingAction.set(null);
    this.pendingDeleteAction.set(null);
    this.pendingEditAction.set(null);
    this.activeEditClosureId.set(closure.id);

    if (closure.date) {
      this.editSiteClosureForm.reset({
        mode: 'date',
        date: closure.date,
        dateDebut: null,
        dateFin: null,
        motif: closure.motif
      });
      this.applyEditModeValidators('date');
      return;
    }

    this.editSiteClosureForm.reset({
      mode: 'period',
      date: null,
      dateDebut: closure.dateDebut,
      dateFin: closure.dateFin,
      motif: closure.motif
    });
    this.applyEditModeValidators('period');
  }

  protected cancelSiteClosureEdit(): void {
    this.activeEditClosureId.set(null);
    this.pendingEditAction.set(null);
    this.siteUpdateError.set('');
    this.editSiteClosureForm.reset({
      mode: 'date',
      date: null,
      dateDebut: null,
      dateFin: null,
      motif: null
    });
    this.applyEditModeValidators('date');
  }

  protected requestSiteClosureUpdate(closure: AdminSiteClosureResponse): void {
    this.successMessage.set('');
    this.siteUpdateError.set('');
    this.pendingEditAction.set(null);
    this.applyEditModeValidators(this.editSiteClosureForm.controls.mode.value);

    if (this.getCurrentSiteId() == null) {
      this.siteUpdateError.set('Aucun site disponible pour cette modification.');
      return;
    }

    if (this.editSiteClosureForm.invalid) {
      this.editSiteClosureForm.markAllAsTouched();
      return;
    }

    this.pendingEditAction.set({ id: closure.id, mode: this.editSiteClosureForm.controls.mode.value });
  }

  protected cancelEditConfirmation(): void {
    this.pendingEditAction.set(null);
  }

  protected confirmSiteClosureUpdate(): void {
    const action = this.pendingEditAction();

    if (!action) {
      return;
    }

    if (action.mode === 'date') {
      this.updateSiteClosureAsDate(action.id);
      return;
    }

    this.updateSiteClosureAsPeriod(action.id);
  }

  protected isEditingSiteClosure(id: number): boolean {
    return this.activeEditClosureId() === id;
  }

  protected isPendingEdit(id: number): boolean {
    return this.pendingEditAction()?.id === id;
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

  protected isEditSiteClosureMode(mode: SiteClosureMode): boolean {
    return this.editSiteClosureForm.controls.mode.value === mode;
  }

  protected showEditDateError(): boolean {
    const control = this.editSiteClosureForm.controls.date;
    return this.editSiteClosureForm.controls.mode.value === 'date' && control.invalid && (control.dirty || control.touched);
  }

  protected showEditDateDebutError(): boolean {
    const control = this.editSiteClosureForm.controls.dateDebut;
    return this.editSiteClosureForm.controls.mode.value === 'period' && control.invalid && (control.dirty || control.touched);
  }

  protected showEditDateFinError(): boolean {
    const control = this.editSiteClosureForm.controls.dateFin;
    return this.editSiteClosureForm.controls.mode.value === 'period' && control.invalid && (control.dirty || control.touched);
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

  private deleteGlobalClosure(id: number): void {
    this.deletingGlobalClosureId.set(id);
    this.globalDeleteError.set('');

    this.adminService
      .deleteGlobalClosure(id)
      .pipe(finalize(() => this.deletingGlobalClosureId.set(null)))
      .subscribe({
        next: () => {
          this.pendingDeleteAction.set(null);
          this.successMessage.set('Fermeture globale supprim\u00e9e.');
          this.loadGlobalClosures();
        },
        error: (error: HttpErrorResponse) => {
          this.globalDeleteError.set(this.getDeleteErrorMessage(error, 'Impossible de supprimer la fermeture globale.'));
        }
      });
  }

  private deleteSiteClosure(id: number): void {
    const siteId = this.getCurrentSiteId();

    if (siteId == null) {
      this.siteDeleteError.set('Aucun site disponible pour cette suppression.');
      this.pendingDeleteAction.set(null);
      return;
    }

    this.deletingSiteClosureId.set(id);
    this.siteDeleteError.set('');

    this.adminService
      .deleteSiteClosure(siteId, id)
      .pipe(finalize(() => this.deletingSiteClosureId.set(null)))
      .subscribe({
        next: () => {
          this.pendingDeleteAction.set(null);
          this.successMessage.set('Fermeture du site supprim\u00e9e.');
          this.loadSiteClosures(siteId);
        },
        error: (error: HttpErrorResponse) => {
          this.siteDeleteError.set(this.getDeleteErrorMessage(error, 'Impossible de supprimer la fermeture du site.'));
        }
      });
  }

  private updateSiteClosureAsDate(id: number): void {
    const siteId = this.getCurrentSiteId();
    const date = this.editSiteClosureForm.controls.date.value;

    if (siteId == null) {
      this.siteUpdateError.set('Aucun site disponible pour cette modification.');
      this.pendingEditAction.set(null);
      return;
    }

    if (!date) {
      this.editSiteClosureForm.controls.date.markAsTouched();
      this.pendingEditAction.set(null);
      return;
    }

    const payload: UpdateSiteDateClosureRequest = {
      date,
      motif: this.normalizeMotif(this.editSiteClosureForm.controls.motif.value)
    };

    this.submitSiteClosureUpdate(siteId, id, this.adminService.updateSiteDateClosure(siteId, id, payload));
  }

  private updateSiteClosureAsPeriod(id: number): void {
    const siteId = this.getCurrentSiteId();
    const dateDebut = this.editSiteClosureForm.controls.dateDebut.value;
    const dateFin = this.editSiteClosureForm.controls.dateFin.value;

    if (siteId == null) {
      this.siteUpdateError.set('Aucun site disponible pour cette modification.');
      this.pendingEditAction.set(null);
      return;
    }

    if (!dateDebut || !dateFin) {
      this.editSiteClosureForm.markAllAsTouched();
      this.pendingEditAction.set(null);
      return;
    }

    const payload: UpdateSitePeriodClosureRequest = {
      dateDebut,
      dateFin,
      motif: this.normalizeMotif(this.editSiteClosureForm.controls.motif.value)
    };

    this.submitSiteClosureUpdate(siteId, id, this.adminService.updateSitePeriodClosure(siteId, id, payload));
  }

  private submitSiteClosureUpdate(siteId: number, id: number, request$: Observable<unknown>): void {
    this.updatingSiteClosureId.set(id);
    this.siteUpdateError.set('');

    request$
      .pipe(finalize(() => this.updatingSiteClosureId.set(null)))
      .subscribe({
        next: () => {
          this.activeEditClosureId.set(null);
          this.pendingEditAction.set(null);
          this.successMessage.set('Fermeture du site modifi\u00e9e.');
          this.loadSiteClosures(siteId);
        },
        error: (error: HttpErrorResponse) => {
          this.siteUpdateError.set(this.getUpdateErrorMessage(error, 'Impossible de modifier la fermeture du site.'));
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

  private applyEditModeValidators(mode: SiteClosureMode): void {
    const dateControl = this.editSiteClosureForm.controls.date;
    const dateDebutControl = this.editSiteClosureForm.controls.dateDebut;
    const dateFinControl = this.editSiteClosureForm.controls.dateFin;

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

  private getDeleteErrorMessage(error: HttpErrorResponse, fallbackMessage: string): string {
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
      return apiError.message || 'Fermeture introuvable.';
    }

    if (apiError.details) {
      return Object.values(apiError.details).join(' ');
    }

    return apiError.message || fallbackMessage;
  }

  private getUpdateErrorMessage(error: HttpErrorResponse, fallbackMessage: string): string {
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
      return apiError.message || 'Fermeture introuvable.';
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
