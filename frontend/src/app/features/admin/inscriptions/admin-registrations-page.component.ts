import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  PendingRegistrationItem,
  RegistrationDecisionResponse,
  RegistrationSubscriptionType,
  ValidateRegistrationRequest
} from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';
import { ApiErrorResponse } from '../../../core/auth/auth.models';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';

@Component({
  selector: 'app-admin-registrations-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-registrations-page.component.html',
  styleUrl: './admin-registrations-page.component.css'
})
export class AdminRegistrationsPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);
  private readonly sitesService = inject(SitesService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly registrations = signal<PendingRegistrationItem[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly feedbackMessage = signal('');
  protected readonly feedbackTone = signal<'success' | 'error' | null>(null);
  protected readonly activeValidationUserId = signal<number | null>(null);
  protected readonly activeDirectValidationUserId = signal<number | null>(null);
  protected readonly activeRejectUserId = signal<number | null>(null);
  protected readonly processingUserId = signal<number | null>(null);
  protected readonly isLoadingSites = signal(true);
  protected readonly sites = signal<SiteOption[]>([]);
  protected readonly sitesErrorMessage = signal('');
  protected readonly subscriptionTypes: Array<{ value: RegistrationSubscriptionType; label: string }> = [
    { value: 'GLOBAL', label: 'Membre global' },
    { value: 'SITE', label: "Membre d'un site" },
    { value: 'LIBRE', label: 'Membre libre' }
  ];

  protected readonly isEmpty = computed(
    () => !this.isLoading() && !this.errorMessage() && this.registrations().length === 0
  );

  protected readonly validationForm = this.fb.group({
    typeAbonnementFinal: this.fb.nonNullable.control<RegistrationSubscriptionType>('GLOBAL', [Validators.required]),
    siteIdFinal: this.fb.control<number | null>(null)
  });

  ngOnInit(): void {
    this.loadSites();
    this.watchValidationTypeChanges();
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

  protected openValidationForm(registration: PendingRegistrationItem): void {
    this.feedbackMessage.set('');
    this.feedbackTone.set(null);
    this.activeDirectValidationUserId.set(null);
    this.activeRejectUserId.set(null);
    this.activeValidationUserId.set(registration.userId);
    this.validationForm.reset({
      typeAbonnementFinal: registration.typeAbonnementDemande,
      siteIdFinal: registration.siteIdDemande
    });
    this.applySiteValidator();
    this.validationForm.markAsPristine();
    this.validationForm.markAsUntouched();
  }

  protected cancelValidationForm(): void {
    this.activeValidationUserId.set(null);
    this.validationForm.reset({
      typeAbonnementFinal: 'GLOBAL',
      siteIdFinal: null
    });
  }

  protected isValidationOpen(userId: number): boolean {
    return this.activeValidationUserId() === userId;
  }

  protected isProcessing(userId: number): boolean {
    return this.processingUserId() === userId;
  }

  protected isDirectValidationConfirmationOpen(userId: number): boolean {
    return this.activeDirectValidationUserId() === userId;
  }

  protected isRejectConfirmationOpen(userId: number): boolean {
    return this.activeRejectUserId() === userId;
  }

  protected isSiteTypeSelected(): boolean {
    return this.validationForm.controls.typeAbonnementFinal.value === 'SITE';
  }

  protected hasSiteFieldError(): boolean {
    const control = this.validationForm.controls.siteIdFinal;
    return control.invalid && (control.touched || control.dirty);
  }

  protected submitValidation(registration: PendingRegistrationItem): void {
    this.feedbackMessage.set('');
    this.feedbackTone.set(null);
    this.applySiteValidator();

    if (this.validationForm.invalid) {
      this.validationForm.markAllAsTouched();
      return;
    }

    this.processingUserId.set(registration.userId);

    this.adminService
      .validateRegistration(registration.userId, this.buildValidationPayload())
      .pipe(finalize(() => this.processingUserId.set(null)))
      .subscribe({
        next: (response) => {
          this.handleActionSuccess(response, 'Demande validee.');
        },
        error: (error: HttpErrorResponse) => {
          this.feedbackTone.set('error');
          this.feedbackMessage.set(this.getActionErrorMessage(error));
        }
      });
  }

  protected openDirectValidationConfirmation(registration: PendingRegistrationItem): void {
    this.feedbackMessage.set('');
    this.feedbackTone.set(null);
    this.activeValidationUserId.set(null);
    this.activeRejectUserId.set(null);
    this.activeDirectValidationUserId.set(registration.userId);
  }

  protected cancelDirectValidationConfirmation(): void {
    this.activeDirectValidationUserId.set(null);
  }

  protected validateRequestedRegistration(registration: PendingRegistrationItem): void {
    this.feedbackMessage.set('');
    this.feedbackTone.set(null);
    this.activeDirectValidationUserId.set(null);
    this.activeRejectUserId.set(null);
    this.activeValidationUserId.set(null);
    this.processingUserId.set(registration.userId);

    this.adminService
      .validateRegistration(registration.userId, this.buildRequestedValidationPayload(registration))
      .pipe(finalize(() => this.processingUserId.set(null)))
      .subscribe({
        next: (response) => {
          this.handleActionSuccess(response, 'Demande validee.');
        },
        error: (error: HttpErrorResponse) => {
          this.feedbackTone.set('error');
          this.feedbackMessage.set(this.getActionErrorMessage(error));
        }
      });
  }

  protected openRejectConfirmation(registration: PendingRegistrationItem): void {
    this.feedbackMessage.set('');
    this.feedbackTone.set(null);
    this.activeValidationUserId.set(null);
    this.activeDirectValidationUserId.set(null);
    this.activeRejectUserId.set(registration.userId);
  }

  protected cancelRejectConfirmation(): void {
    this.activeRejectUserId.set(null);
  }

  protected confirmRejectRegistration(registration: PendingRegistrationItem): void {
    this.feedbackMessage.set('');
    this.feedbackTone.set(null);
    this.processingUserId.set(registration.userId);

    this.adminService
      .rejectRegistration(registration.userId)
      .pipe(finalize(() => this.processingUserId.set(null)))
      .subscribe({
        next: (response) => {
          this.handleActionSuccess(response, 'Demande refusee.');
        },
        error: (error: HttpErrorResponse) => {
          this.feedbackTone.set('error');
          this.feedbackMessage.set(this.getActionErrorMessage(error));
        }
      });
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

  private loadSites(): void {
    this.isLoadingSites.set(true);
    this.sitesErrorMessage.set('');

    this.sitesService
      .getSites()
      .pipe(finalize(() => this.isLoadingSites.set(false)))
      .subscribe({
        next: (sites) => {
          this.sites.set(sites);
        },
        error: (error: HttpErrorResponse) => {
          this.sites.set([]);
          this.sitesErrorMessage.set(this.getSitesErrorMessage(error));
        }
      });
  }

  private watchValidationTypeChanges(): void {
    this.validationForm.controls.typeAbonnementFinal.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.applySiteValidator();
      });
  }

  private applySiteValidator(): void {
    const siteControl = this.validationForm.controls.siteIdFinal;

    if (this.isSiteTypeSelected()) {
      siteControl.setValidators([Validators.required]);
    } else {
      siteControl.clearValidators();
      siteControl.setValue(null);
    }

    siteControl.updateValueAndValidity({ emitEvent: false });
  }

  private buildValidationPayload(): ValidateRegistrationRequest {
    const rawValue = this.validationForm.getRawValue();
    const payload: ValidateRegistrationRequest = {
      typeAbonnementFinal: rawValue.typeAbonnementFinal
    };

    if (rawValue.typeAbonnementFinal === 'SITE' && rawValue.siteIdFinal != null) {
      payload.siteIdFinal = rawValue.siteIdFinal;
    }

    return payload;
  }

  private buildRequestedValidationPayload(registration: PendingRegistrationItem): ValidateRegistrationRequest {
    const payload: ValidateRegistrationRequest = {
      typeAbonnementFinal: registration.typeAbonnementDemande
    };

    if (registration.typeAbonnementDemande === 'SITE' && registration.siteIdDemande != null) {
      payload.siteIdFinal = registration.siteIdDemande;
    }

    return payload;
  }

  private handleActionSuccess(response: RegistrationDecisionResponse, fallbackMessage: string): void {
    this.feedbackTone.set('success');
    this.feedbackMessage.set(response.message || fallbackMessage);
    this.activeDirectValidationUserId.set(null);
    this.activeRejectUserId.set(null);
    this.cancelValidationForm();
    this.loadPendingRegistrations();
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

  private getActionErrorMessage(error: HttpErrorResponse): string {
    const apiError = error.error as ApiErrorResponse | null;
    const backendMessage = apiError?.message ?? '';

    if (error.status === 0) {
      return 'Backend inaccessible.';
    }

    if (error.status === 404) {
      if (backendMessage.includes("Demande d'inscription introuvable")) {
        return 'Demande introuvable.';
      }

      if (backendMessage.includes('Site introuvable')) {
        return 'Site introuvable.';
      }

      return backendMessage || 'Ressource introuvable.';
    }

    if (error.status === 400) {
      if (apiError?.details?.['typeAbonnementFinal']) {
        return 'Le type final est obligatoire.';
      }

      if (backendMessage.includes("n'est plus en attente")) {
        return 'Demande deja traitee.';
      }

      if (backendMessage.includes('siteIdFinal') && backendMessage.includes('obligatoire')) {
        return 'Le site est obligatoire pour un membre de site.';
      }

      if (backendMessage.includes('siteIdFinal') && backendMessage.includes('interdit')) {
        return 'Aucun site ne doit etre envoye pour ce type.';
      }

      return backendMessage || 'Demande invalide.';
    }

    if (error.status === 401) {
      return 'Authentification requise.';
    }

    if (error.status === 403) {
      return 'Acces administrateur refuse.';
    }

    return "Impossible de traiter cette demande pour le moment.";
  }

  private getSitesErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'La liste des sites est momentanement indisponible.';
    }

    return 'Impossible de charger les sites pour le moment.';
  }
}
