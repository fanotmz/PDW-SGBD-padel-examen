import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiErrorResponse, LoginRequest } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css'
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly showPassword = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const payload: LoginRequest = this.form.getRawValue();

    this.authService
      .login(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.router.navigateByUrl(this.getTargetUrl());
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.buildErrorMessage(error));
        }
      });
  }

  protected togglePasswordVisibility(): void {
    this.showPassword.update((value) => !value);
  }

  private buildErrorMessage(error: HttpErrorResponse): string {
    const apiError = error.error as ApiErrorResponse | null;

    if (error.status === 0) {
      return 'Backend inaccessible.';
    }

    if (error.status === 401) {
      return 'Identifiant ou mot de passe invalide.';
    }

    if (error.status === 403 && apiError?.message) {
      return apiError.message;
    }

    if (error.status === 400) {
      const details = apiError?.details;

      if (details) {
        return Object.values(details).join(' ');
      }

      return apiError?.message ?? 'Requête invalide.';
    }

    return 'Backend inaccessible ou erreur inattendue.';
  }

  private getTargetUrl(): string {
    if (this.authService.isAdmin()) {
      return '/admin';
    }

    if (this.authService.hasPlayerProfile()) {
      return '/espace-prive';
    }

    return '/';
  }
}
