import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, map, of, switchMap } from 'rxjs';
import {
  AdminInfoResponse,
  AdminSiteScheduleResponse,
  UpsertSiteScheduleRequest
} from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';
import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';

@Component({
  selector: 'app-admin-schedules-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-schedules-page.component.html',
  styleUrl: './admin-schedules-page.component.css'
})
export class AdminSchedulesPageComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly sitesService = inject(SitesService);
  private readonly fb = inject(FormBuilder);

  protected readonly adminInfo = signal<AdminInfoResponse | null>(null);
  protected readonly sites = signal<SiteOption[]>([]);
  protected readonly selectedSiteId = signal<number | null>(null);
  protected readonly schedules = signal<AdminSiteScheduleResponse[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly isLoadingSchedules = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly schedulesError = signal('');
  protected readonly successMessage = signal('');
  protected readonly createError = signal('');
  protected readonly updateError = signal('');
  protected readonly deleteError = signal('');
  protected readonly pendingCreateConfirmation = signal(false);
  protected readonly activeEditScheduleId = signal<number | null>(null);
  protected readonly pendingUpdateScheduleId = signal<number | null>(null);
  protected readonly pendingDeleteScheduleId = signal<number | null>(null);
  protected readonly isSubmittingCreate = signal(false);
  protected readonly isSubmittingUpdate = signal(false);
  protected readonly deletingScheduleId = signal<number | null>(null);

  protected readonly createScheduleForm = this.fb.group({
    annee: this.fb.nonNullable.control('', [Validators.required]),
    heureOuverture: this.fb.nonNullable.control('', [Validators.required]),
    heureFermeture: this.fb.nonNullable.control('', [Validators.required])
  });

  protected readonly editScheduleForm = this.fb.group({
    annee: this.fb.nonNullable.control('', [Validators.required]),
    heureOuverture: this.fb.nonNullable.control('', [Validators.required]),
    heureFermeture: this.fb.nonNullable.control('', [Validators.required])
  });

  protected readonly isAdminGlobal = computed(() => this.adminInfo()?.adminType === 'GLOBAL');
  protected readonly canManageSchedules = computed(() => this.getCurrentSiteId() != null);
  protected readonly hasNoSchedules = computed(
    () => !this.isLoadingSchedules() && !this.schedulesError() && this.schedules().length === 0
  );

  protected readonly selectedSiteName = computed(() => {
    const adminInfo = this.adminInfo();

    if (adminInfo?.adminType === 'SITE') {
      return adminInfo.siteNom ?? 'Site administré';
    }

    const selectedSiteId = this.selectedSiteId();

    if (selectedSiteId == null) {
      return '';
    }

    return this.sites().find((site) => site.id === selectedSiteId)?.nom ?? `Site ${selectedSiteId}`;
  });

  ngOnInit(): void {
    this.loadInitialData();
  }

  protected onSiteChange(value: string): void {
    const siteId = Number(value);
    this.resetActionState();

    if (Number.isNaN(siteId)) {
      this.selectedSiteId.set(null);
      this.schedules.set([]);
      this.schedulesError.set('');
      return;
    }

    this.selectedSiteId.set(siteId);
    this.loadSchedules(siteId);
  }

  protected requestCreateSchedule(): void {
    this.successMessage.set('');
    this.createError.set('');
    this.pendingCreateConfirmation.set(false);

    if (this.createScheduleForm.invalid) {
      this.createScheduleForm.markAllAsTouched();
      return;
    }

    if (this.getCurrentSiteId() == null) {
      this.createError.set('Périmètre administrateur introuvable.');
      return;
    }

    this.pendingCreateConfirmation.set(true);
  }

  protected confirmCreateSchedule(): void {
    const siteId = this.getCurrentSiteId();

    if (siteId == null) {
      this.createError.set('Périmètre administrateur introuvable.');
      return;
    }

    this.isSubmittingCreate.set(true);
    this.createError.set('');

    this.adminService
      .createSiteSchedule(siteId, this.getCreatePayload())
      .pipe(finalize(() => this.isSubmittingCreate.set(false)))
      .subscribe({
        next: () => {
          this.successMessage.set('Horaire créé.');
          this.pendingCreateConfirmation.set(false);
          this.createScheduleForm.reset();
          this.loadSchedules(siteId);
        },
        error: (error: HttpErrorResponse) => {
          this.createError.set(this.getErrorMessage(error, "Impossible de créer l'horaire."));
        }
      });
  }

  protected cancelCreateConfirmation(): void {
    this.pendingCreateConfirmation.set(false);
  }

  protected startEditSchedule(schedule: AdminSiteScheduleResponse): void {
    this.successMessage.set('');
    this.updateError.set('');
    this.deleteError.set('');
    this.pendingUpdateScheduleId.set(null);
    this.pendingDeleteScheduleId.set(null);
    this.activeEditScheduleId.set(schedule.id);
    this.editScheduleForm.setValue({
      annee: String(schedule.annee),
      heureOuverture: this.toTimeInputValue(schedule.heureOuverture),
      heureFermeture: this.toTimeInputValue(schedule.heureFermeture)
    });
  }

  protected requestUpdateSchedule(schedule: AdminSiteScheduleResponse): void {
    this.successMessage.set('');
    this.updateError.set('');
    this.pendingUpdateScheduleId.set(null);

    if (!this.isEditingSchedule(schedule.id)) {
      this.startEditSchedule(schedule);
    }

    if (this.editScheduleForm.invalid) {
      this.editScheduleForm.markAllAsTouched();
      return;
    }

    if (this.getCurrentSiteId() == null) {
      this.updateError.set('Périmètre administrateur introuvable.');
      return;
    }

    this.pendingUpdateScheduleId.set(schedule.id);
  }

  protected confirmUpdateSchedule(schedule: AdminSiteScheduleResponse): void {
    const siteId = this.getCurrentSiteId();

    if (siteId == null) {
      this.updateError.set('Périmètre administrateur introuvable.');
      return;
    }

    this.isSubmittingUpdate.set(true);
    this.updateError.set('');

    this.adminService
      .updateSiteSchedule(siteId, schedule.id, this.getEditPayload())
      .pipe(finalize(() => this.isSubmittingUpdate.set(false)))
      .subscribe({
        next: () => {
          this.successMessage.set('Horaire modifié.');
          this.cancelEditSchedule();
          this.loadSchedules(siteId);
        },
        error: (error: HttpErrorResponse) => {
          this.updateError.set(this.getErrorMessage(error, "Impossible de modifier l'horaire."));
        }
      });
  }

  protected cancelEditSchedule(): void {
    this.activeEditScheduleId.set(null);
    this.pendingUpdateScheduleId.set(null);
    this.updateError.set('');
    this.editScheduleForm.reset();
  }

  protected requestDeleteSchedule(schedule: AdminSiteScheduleResponse): void {
    this.successMessage.set('');
    this.deleteError.set('');
    this.pendingDeleteScheduleId.set(schedule.id);
    this.pendingUpdateScheduleId.set(null);
  }

  protected confirmDeleteSchedule(schedule: AdminSiteScheduleResponse): void {
    const siteId = this.getCurrentSiteId();

    if (siteId == null) {
      this.deleteError.set('Périmètre administrateur introuvable.');
      return;
    }

    this.deletingScheduleId.set(schedule.id);
    this.deleteError.set('');

    this.adminService
      .deleteSiteSchedule(siteId, schedule.id)
      .pipe(finalize(() => this.deletingScheduleId.set(null)))
      .subscribe({
        next: () => {
          this.successMessage.set('Horaire supprimé.');
          this.pendingDeleteScheduleId.set(null);
          this.loadSchedules(siteId);
        },
        error: (error: HttpErrorResponse) => {
          this.deleteError.set(this.getErrorMessage(error, "Impossible de supprimer l'horaire."));
        }
      });
  }

  protected cancelDeleteConfirmation(): void {
    this.pendingDeleteScheduleId.set(null);
    this.deleteError.set('');
  }

  protected isEditingSchedule(scheduleId: number): boolean {
    return this.activeEditScheduleId() === scheduleId;
  }

  protected isPendingUpdate(scheduleId: number): boolean {
    return this.pendingUpdateScheduleId() === scheduleId;
  }

  protected isPendingDelete(scheduleId: number): boolean {
    return this.pendingDeleteScheduleId() === scheduleId;
  }

  protected getDeleteConfirmationMessage(schedule: AdminSiteScheduleResponse): string {
    const isOnlyScheduleForYear = this.schedules().filter((candidate) => candidate.annee === schedule.annee).length === 1;

    if (isOnlyScheduleForYear) {
      return "Confirmer la suppression de cet horaire ? Attention : c'est le seul horaire configuré pour cette année sur ce site. Les futures créations de matchs pour cette année pourraient être bloquées.";
    }

    return 'Confirmer la suppression de cet horaire ?';
  }

  protected showCreateYearError(): boolean {
    return this.shouldShowFieldError(this.createScheduleForm.controls.annee);
  }

  protected showCreateOpeningError(): boolean {
    return this.shouldShowFieldError(this.createScheduleForm.controls.heureOuverture);
  }

  protected showCreateClosingError(): boolean {
    return this.shouldShowFieldError(this.createScheduleForm.controls.heureFermeture);
  }

  protected showEditYearError(): boolean {
    return this.shouldShowFieldError(this.editScheduleForm.controls.annee);
  }

  protected showEditOpeningError(): boolean {
    return this.shouldShowFieldError(this.editScheduleForm.controls.heureOuverture);
  }

  protected showEditClosingError(): boolean {
    return this.shouldShowFieldError(this.editScheduleForm.controls.heureFermeture);
  }

  protected formatTime(value: string): string {
    if (!value) {
      return 'Non renseigné';
    }

    return value.length >= 5 ? value.slice(0, 5) : value;
  }

  protected trackSchedule(schedule: AdminSiteScheduleResponse): number {
    return schedule.id;
  }

  private loadInitialData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.schedulesError.set('');

    this.adminService
      .getInfo()
      .pipe(
        switchMap((adminInfo) => {
          this.adminInfo.set(adminInfo);

          if (adminInfo.adminType === 'GLOBAL') {
            return this.sitesService.getSites();
          }

          if (adminInfo.siteId == null) {
            throw new Error('Périmètre administrateur introuvable.');
          }

          return of([] as SiteOption[]);
        }),
        map((sites) => {
          this.sites.set(sites);
          return sites;
        }),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (sites) => {
          const adminInfo = this.adminInfo();

          if (adminInfo?.adminType === 'GLOBAL') {
            this.initializeGlobalSiteSelection(sites);
            return;
          }

          if (adminInfo?.adminType === 'SITE' && adminInfo.siteId != null) {
            this.selectedSiteId.set(adminInfo.siteId);
            this.loadSchedules(adminInfo.siteId);
          }
        },
        error: (error: HttpErrorResponse | Error) => {
          this.schedules.set([]);
          this.errorMessage.set(this.getErrorMessage(error, "Impossible de charger la page des horaires."));
        }
      });
  }

  private initializeGlobalSiteSelection(sites: SiteOption[]): void {
    if (sites.length === 0) {
      this.selectedSiteId.set(null);
      this.schedules.set([]);
      this.errorMessage.set('Aucun site disponible.');
      return;
    }

    const currentSiteId = this.selectedSiteId();
    const selectedSiteId = currentSiteId != null && sites.some((site) => site.id === currentSiteId)
      ? currentSiteId
      : sites[0].id;

    this.selectedSiteId.set(selectedSiteId);
    this.loadSchedules(selectedSiteId);
  }

  private loadSchedules(siteId: number): void {
    this.isLoadingSchedules.set(true);
    this.schedulesError.set('');

    this.adminService
      .getSiteSchedules(siteId)
      .pipe(finalize(() => this.isLoadingSchedules.set(false)))
      .subscribe({
        next: (schedules) => {
          this.schedules.set(schedules);
        },
        error: (error: HttpErrorResponse) => {
          this.schedules.set([]);
          this.schedulesError.set(this.getErrorMessage(error, 'Impossible de charger les horaires.'));
        }
      });
  }

  private resetActionState(): void {
    this.successMessage.set('');
    this.createError.set('');
    this.updateError.set('');
    this.deleteError.set('');
    this.pendingCreateConfirmation.set(false);
    this.pendingUpdateScheduleId.set(null);
    this.pendingDeleteScheduleId.set(null);
    this.activeEditScheduleId.set(null);
    this.createScheduleForm.reset();
    this.editScheduleForm.reset();
  }

  private getCurrentSiteId(): number | null {
    const adminInfo = this.adminInfo();

    if (adminInfo?.adminType === 'SITE') {
      return adminInfo.siteId;
    }

    return this.selectedSiteId();
  }

  private getCreatePayload(): UpsertSiteScheduleRequest {
    return this.getPayloadFromForm(this.createScheduleForm.getRawValue());
  }

  private getEditPayload(): UpsertSiteScheduleRequest {
    return this.getPayloadFromForm(this.editScheduleForm.getRawValue());
  }

  private getPayloadFromForm(value: {
    annee: string;
    heureOuverture: string;
    heureFermeture: string;
  }): UpsertSiteScheduleRequest {
    return {
      annee: Number(value.annee),
      heureOuverture: value.heureOuverture,
      heureFermeture: value.heureFermeture
    };
  }

  private toTimeInputValue(value: string): string {
    return value.length >= 5 ? value.slice(0, 5) : value;
  }

  private shouldShowFieldError(control: { invalid: boolean; touched: boolean; dirty: boolean }): boolean {
    return control.invalid && (control.touched || control.dirty);
  }

  private getErrorMessage(error: HttpErrorResponse | Error, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'Backend inaccessible.';
      }

      if (error.status === 401) {
        return 'Authentification requise.';
      }

      if (error.status === 403) {
        return 'Accès administrateur refusé.';
      }

      if (error.status === 404) {
        return 'Site introuvable.';
      }

      const backendMessage = this.getBackendMessage(error);

      return backendMessage || fallback;
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
