import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CreatedMatch, CreateMatchPayload, MatchVisibility } from '../../../core/matches/create-match.models';
import { MatchCreationService } from '../../../core/matches/match-creation.service';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';
import { TerrainOption } from '../../../core/terrains/terrain.models';
import { TerrainsService } from '../../../core/terrains/terrains.service';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

interface FriendlyErrorDetail {
  key: string;
  message: string;
}

@Component({
  selector: 'app-create-match-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DatePipe, CurrencyPipe],
  templateUrl: './create-match-page.component.html',
  styleUrl: './create-match-page.component.css'
})
export class CreateMatchPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matchCreationService = inject(MatchCreationService);
  private readonly sitesService = inject(SitesService);
  private readonly terrainsService = inject(TerrainsService);

  protected readonly sites = signal<SiteOption[]>([]);
  protected readonly terrains = signal<TerrainOption[]>([]);
  protected readonly isLoadingSites = signal(true);
  protected readonly isLoadingTerrains = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly globalErrorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly createdMatch = signal<CreatedMatch | null>(null);
  protected readonly backendFieldErrors = signal<Record<string, string>>({});
  protected readonly friendlyErrorDetails = signal<FriendlyErrorDetail[]>([]);
  protected readonly timeSlots = this.buildTimeSlots();

  protected readonly form = this.fb.nonNullable.group({
    siteId: [null as number | null, Validators.required],
    terrainId: [{ value: null as number | null, disabled: true }, Validators.required],
    date: ['', Validators.required],
    heure: ['', Validators.required],
    visibilite: ['PUBLIC' as MatchVisibility, Validators.required]
  });

  ngOnInit(): void {
    this.loadSites();

    this.form.controls.siteId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((siteId) => {
        this.form.controls.terrainId.reset(null);
        this.backendFieldErrors.set({});
        this.friendlyErrorDetails.set([]);

        if (siteId == null) {
          this.form.controls.terrainId.disable();
          this.terrains.set([]);
          return;
        }

        this.form.controls.terrainId.disable();
        this.loadTerrains(siteId);
      });
  }

  protected canSubmit(): boolean {
    return (
      this.form.valid &&
      !this.isLoadingSites() &&
      !this.isLoadingTerrains() &&
      !this.isSubmitting()
    );
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    this.globalErrorMessage.set('');
    this.successMessage.set('');
    this.createdMatch.set(null);
    this.backendFieldErrors.set({});
    this.friendlyErrorDetails.set([]);

    if (this.isLoadingSites() || this.isLoadingTerrains() || this.isSubmitting()) {
      return;
    }

    if (this.form.invalid) {
      return;
    }

    const payload = this.buildPayload();
    if (!payload) {
      this.globalErrorMessage.set('Le formulaire est incomplet.');
      return;
    }

    this.isSubmitting.set(true);

    this.matchCreationService
      .createMatch(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (createdMatch) => {
          this.createdMatch.set(createdMatch);
          this.successMessage.set('Match créé avec succès.');
          this.form.controls.date.reset('');
          this.form.controls.heure.reset('');
          this.form.controls.visibilite.setValue('PUBLIC');
          this.form.controls.terrainId.reset(null);
        },
        error: (error: HttpErrorResponse) => {
          this.handleSubmitError(error);
        }
      });
  }

  protected siteLabel(site: SiteOption): string {
    return `${site.nom} - ${site.ville}`;
  }

  protected getFieldError(field: 'siteId' | 'terrainId' | 'date' | 'heure' | 'visibilite'): string {
    const control = this.form.controls[field];

    if ((control.touched || control.dirty) && control.hasError('required')) {
      switch (field) {
        case 'siteId':
          return 'Le site est obligatoire.';
        case 'terrainId':
          return 'Le terrain est obligatoire.';
        case 'date':
          return 'La date est obligatoire.';
        case 'heure':
          return "L'heure est obligatoire.";
        case 'visibilite':
          return 'La visibilité est obligatoire.';
      }
    }

    return '';
  }

  protected getSubmitHints(): string[] {
    const hints: string[] = [];

    if (this.isLoadingSites()) {
      hints.push('Chargement des sites en cours.');
    }

    if (this.isLoadingTerrains()) {
      hints.push('Chargement des terrains en cours.');
    }

    if (this.isSubmitting()) {
      hints.push('Création en cours...');
    }

    if (this.form.controls.siteId.hasError('required')) {
      hints.push('Sélectionnez un site.');
    }

    if (
      this.form.controls.siteId.value != null &&
      this.form.controls.terrainId.disabled &&
      !this.isLoadingTerrains()
    ) {
      hints.push('Sélectionnez un terrain disponible.');
    } else if (this.form.controls.terrainId.hasError('required')) {
      hints.push('Sélectionnez un terrain.');
    }

    if (this.form.controls.date.hasError('required')) {
      hints.push('Sélectionnez une date.');
    }

    if (this.form.controls.heure.hasError('required')) {
      hints.push('Sélectionnez une heure.');
    }

    if (this.form.controls.visibilite.hasError('required')) {
      hints.push('Sélectionnez une visibilité.');
    }

    return Array.from(new Set(hints));
  }

  protected dateDebutFieldError(): string {
    return this.backendFieldErrors()['dateDebut'] ?? '';
  }

  private loadSites(): void {
    this.isLoadingSites.set(true);
    this.globalErrorMessage.set('');

    this.sitesService
      .getSites()
      .pipe(finalize(() => this.isLoadingSites.set(false)))
      .subscribe({
        next: (sites) => {
          this.sites.set(sites);
        },
        error: (error: HttpErrorResponse) => {
          this.globalErrorMessage.set(
            error.status === 0 ? 'Backend inaccessible.' : 'Impossible de charger les sites.'
          );
        }
      });
  }

  private loadTerrains(siteId: number): void {
    this.isLoadingTerrains.set(true);
    this.globalErrorMessage.set('');

    this.terrainsService
      .getTerrains(siteId)
      .pipe(finalize(() => this.isLoadingTerrains.set(false)))
      .subscribe({
        next: (terrains) => {
          this.terrains.set(terrains);

          if (terrains.length > 0) {
            this.form.controls.terrainId.enable();
          } else {
            this.form.controls.terrainId.disable();
          }
        },
        error: (error: HttpErrorResponse) => {
          this.globalErrorMessage.set(
            error.status === 0 ? 'Backend inaccessible.' : 'Impossible de charger les terrains.'
          );
          this.terrains.set([]);
          this.form.controls.terrainId.disable();
        }
      });
  }

  private buildPayload(): CreateMatchPayload | null {
    const { terrainId, date, heure, visibilite } = this.form.getRawValue();

    if (terrainId == null || !date || !heure) {
      return null;
    }

    return {
      terrainId,
      dateDebut: `${date}T${heure}:00`,
      visibilite
    };
  }

  private handleSubmitError(error: HttpErrorResponse): void {
    const apiError = this.normalizeApiErrorBody(error.error);

    if (error.status === 0) {
      this.globalErrorMessage.set('Backend inaccessible.');
      return;
    }

    if (apiError.details) {
      this.backendFieldErrors.set(apiError.details);
      this.friendlyErrorDetails.set(this.mapFriendlyErrorDetails(apiError.details));

      if (apiError.details['dateDebut']) {
        this.globalErrorMessage.set('Impossible de créer ce match.');
        return;
      }
    }

    if (apiError.message) {
      this.globalErrorMessage.set(apiError.message);
      return;
    }

    if (error.status === 404) {
      this.globalErrorMessage.set('Terrain introuvable.');
      return;
    }

    this.globalErrorMessage.set('Impossible de créer le match.');
  }

  private normalizeApiErrorBody(raw: unknown): ApiErrorBody {
    if (typeof raw === 'string') {
      try {
        return JSON.parse(raw) as ApiErrorBody;
      } catch {
        return { message: raw };
      }
    }

    if (raw && typeof raw === 'object') {
      return raw as ApiErrorBody;
    }

    return {};
  }

  private mapFriendlyErrorDetails(details: Record<string, string>): FriendlyErrorDetail[] {
    const friendlyDetails: FriendlyErrorDetail[] = [];

    if (details['dateDebut']) {
      friendlyDetails.push({
        key: 'dateDebut',
        message: 'La date et l’heure du match doivent être dans le futur.'
      });
    }

    return friendlyDetails;
  }

  private buildTimeSlots(): string[] {
    const slots: string[] = [];
    const openingHour = 8;
    const closingHour = 22;

    for (let hour = openingHour; hour < closingHour; hour += 1) {
      for (let minute = 0; minute < 60; minute += 15) {
        const hh = hour.toString().padStart(2, '0');
        const mm = minute.toString().padStart(2, '0');
        slots.push(`${hh}:${mm}`);
      }
    }

    slots.push(`${closingHour.toString().padStart(2, '0')}:00`);

    return slots;
  }
}
