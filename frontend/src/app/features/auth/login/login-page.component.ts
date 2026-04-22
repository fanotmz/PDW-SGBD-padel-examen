import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
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
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected isSubmitting = false;
  protected errorMessage = '';

  protected readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const payload: LoginRequest = this.form.getRawValue();

    this.authService.login(payload).subscribe({
      next: () => {
        this.router.navigateByUrl(this.getTargetUrl());
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting = false;
        this.errorMessage = this.buildErrorMessage(error);
      }
    });
  }

  private buildErrorMessage(error: HttpErrorResponse): string {
    const apiError = error.error as ApiErrorResponse | null;

    if (error.status === 401) {
      return 'Identifiants invalides.';
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
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

    if (returnUrl?.startsWith('/')) {
      return returnUrl;
    }

    return '/';
  }
}
