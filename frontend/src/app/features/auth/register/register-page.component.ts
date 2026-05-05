import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  ApiErrorResponse,
  PlayerSubscriptionType,
  RegisterRequest,
  RegisterResponse
} from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.css'
})
export class RegisterPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly sitesService = inject(SitesService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly isSubmitting = signal(false);
  protected readonly isLoadingSites = signal(true);
  protected readonly sites = signal<SiteOption[]>([]);
  protected readonly errorMessage = signal('');
  protected readonly sitesErrorMessage = signal('');
  protected readonly successResponse = signal<RegisterResponse | null>(null);
  protected readonly showPassword = signal(false);
  protected readonly showConfirmPassword = signal(false);

  protected readonly playerTypes: Array<{ value: PlayerSubscriptionType; label: string }> = [
    { value: 'GLOBAL', label: 'Membre global - accès à tous les sites' },
    { value: 'SITE', label: 'Membre d’un site - rattachement à un site spécifique' },
    { value: 'LIBRE', label: 'Membre libre - accès selon les délais de réservation' }
  ];

  protected readonly form = this.fb.group(
    {
      nom: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
      username: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(100)]),
      password: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
      confirmPassword: this.fb.nonNullable.control('', [Validators.required]),
      typeAbonnementDemande: this.fb.nonNullable.control<PlayerSubscriptionType>('GLOBAL', [Validators.required]),
      siteIdDemande: this.fb.control<number | null>(null)
    },
    { validators: passwordMatchValidator() }
  );

  ngOnInit(): void {
    this.loadSites();
    this.watchTypeChanges();
  }

  protected onSubmit(): void {
    this.applySiteValidator();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.authService
      .register(this.buildPayload())
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.successResponse.set(response);
          this.form.reset({
            nom: '',
            username: '',
            password: '',
            confirmPassword: '',
            typeAbonnementDemande: 'GLOBAL',
            siteIdDemande: null
          });
          this.form.markAsPristine();
          this.form.markAsUntouched();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.buildErrorMessage(error));
        }
      });
  }

  protected togglePasswordVisibility(): void {
    this.showPassword.update((value) => !value);
  }

  protected toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword.update((value) => !value);
  }

  protected isSiteTypeSelected(): boolean {
    return this.form.controls.typeAbonnementDemande.value === 'SITE';
  }

  protected showFieldError(fieldName: 'nom' | 'username' | 'password' | 'confirmPassword' | 'siteIdDemande'): boolean {
    const control = this.form.controls[fieldName];
    return control.invalid && (control.touched || control.dirty);
  }

  protected showPasswordMismatchError(): boolean {
    return this.form.hasError('passwordMismatch') && this.form.controls.confirmPassword.touched;
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
          this.sitesErrorMessage.set(this.buildSitesErrorMessage(error));
        }
      });
  }

  private watchTypeChanges(): void {
    this.form.controls.typeAbonnementDemande.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.errorMessage.set('');
        this.applySiteValidator();
      });
  }

  private applySiteValidator(): void {
    const siteControl = this.form.controls.siteIdDemande;

    if (this.isSiteTypeSelected()) {
      siteControl.setValidators([Validators.required]);
    } else {
      siteControl.clearValidators();
      siteControl.setValue(null);
    }

    siteControl.updateValueAndValidity({ emitEvent: false });
  }

  private buildPayload(): RegisterRequest {
    const rawValue = this.form.getRawValue();
    const payload: RegisterRequest = {
      nom: rawValue.nom.trim(),
      username: rawValue.username.trim(),
      password: rawValue.password,
      typeAbonnementDemande: rawValue.typeAbonnementDemande
    };

    if (rawValue.typeAbonnementDemande === 'SITE' && rawValue.siteIdDemande != null) {
      payload.siteIdDemande = rawValue.siteIdDemande;
    }

    return payload;
  }

  private buildErrorMessage(error: HttpErrorResponse): string {
    const apiError = error.error as ApiErrorResponse | null;
    const backendMessage = apiError?.message ?? '';

    if (error.status === 0) {
      return 'Backend inaccessible.';
    }

    if (error.status === 404) {
      return 'Le site sélectionné est introuvable.';
    }

    if (error.status === 400) {
      if (apiError?.details) {
        return this.buildValidationDetailsMessage(apiError.details);
      }

      if (backendMessage.includes('Username') && backendMessage.includes('utilise')) {
        return 'Ce login est déjà utilisé.';
      }

      if (backendMessage.includes("siteIdDemande") && backendMessage.includes('obligatoire')) {
        return 'Veuillez sélectionner un site pour une demande de type membre d’un site.';
      }

      if (backendMessage.includes("siteIdDemande") && backendMessage.includes('interdit')) {
        return 'Le site demandé ne correspond pas au type de profil sélectionné.';
      }

      return backendMessage || 'Demande invalide.';
    }

    return 'Impossible d’envoyer votre demande pour le moment.';
  }

  private buildValidationDetailsMessage(details: Record<string, string>): string {
    if (details['nom']) {
      return 'Le nom est obligatoire.';
    }

    if (details['username']) {
      return 'Le login est obligatoire.';
    }

    if (details['password']) {
      return 'Le mot de passe est obligatoire.';
    }

    if (details['typeAbonnementDemande']) {
      return 'Le type de profil demandé est obligatoire.';
    }

    if (details['siteIdDemande']) {
      return 'Veuillez sélectionner un site.';
    }

    return 'Certains champs du formulaire sont invalides.';
  }

  private buildSitesErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'La liste des sites est momentanément indisponible.';
    }

    return 'Impossible de charger les sites pour le moment.';
  }
}

function passwordMatchValidator(): ValidatorFn {
  return (control): ValidationErrors | null => {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (password === confirmPassword) {
      return null;
    }

    return { passwordMismatch: true };
  };
}
