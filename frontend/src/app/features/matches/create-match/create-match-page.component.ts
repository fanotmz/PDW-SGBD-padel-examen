import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  CreatedMatch,
  CreateMatchPayload,
  MatchSlotsResponse,
  MatchVisibility
} from '../../../core/matches/create-match.models';
import { MatchCreationService } from '../../../core/matches/match-creation.service';
import { MeProfile } from '../../../core/me/me.models';
import { MeService } from '../../../core/me/me.service';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';
import { TerrainOption } from '../../../core/terrains/terrain.models';
import { TerrainsService } from '../../../core/terrains/terrains.service';
import { BELGIAN_DATE_PROVIDERS, formatDateForApi } from '../../../shared/date/belgian-date-formats';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { PageStateComponent } from '../../../shared/ui/page-state/page-state.component';
import { UiFeedbackService } from '../../../shared/ui/ui-feedback.service';
import { UI_MESSAGES } from '../../../shared/ui/ui-messages';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

interface FriendlyErrorDetail {
  key: string;
  message: string;
}

interface SlotScheduleInfo {
  annee: number;
  heureOuverture: string;
  heureFermeture: string;
  dureeMatchMinutes: number;
  bufferMinutes: number;
}

@Component({
  selector: 'app-create-match-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DatePipe,
    CurrencyPipe,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule,
    PageHeaderComponent,
    PageStateComponent
  ],
  providers: BELGIAN_DATE_PROVIDERS,
  templateUrl: './create-match-page.component.html',
  styleUrl: './create-match-page.component.css'
})
export class CreateMatchPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matchCreationService = inject(MatchCreationService);
  private readonly meService = inject(MeService);
  private readonly sitesService = inject(SitesService);
  private readonly terrainsService = inject(TerrainsService);
  private readonly feedback = inject(UiFeedbackService);

  protected readonly sites = signal<SiteOption[]>([]);
  protected readonly terrains = signal<TerrainOption[]>([]);
  protected readonly currentProfile = signal<MeProfile | null>(null);
  protected readonly isLoadingProfile = signal(true);
  protected readonly isLoadingSites = signal(true);
  protected readonly isLoadingTerrains = signal(false);
  protected readonly isLoadingSlots = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly globalErrorMessage = signal('');
  protected readonly slotsMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly createdMatch = signal<CreatedMatch | null>(null);
  protected readonly backendFieldErrors = signal<Record<string, string>>({});
  protected readonly friendlyErrorDetails = signal<FriendlyErrorDetail[]>([]);
  protected readonly availableSlots = signal<string[]>([]);
  protected readonly slotScheduleInfo = signal<SlotScheduleInfo | null>(null);
  protected readonly uiMessages = UI_MESSAGES;
  protected readonly lastAvailableSlot = computed(() => {
    const slots = this.availableSlots();
    return slots.length > 0 ? slots[slots.length - 1] : '';
  });
  protected readonly isSiteProfileLocked = computed(() => {
    const profile = this.currentProfile();
    return profile?.type === 'SITE' && profile.siteId != null;
  });
  protected readonly selectedSiteLabel = computed(() => {
    const profileSiteId = this.currentProfile()?.siteId;
    const selectedSiteId = profileSiteId ?? this.form.controls.siteId.value;
    const site = this.sites().find((option) => option.id === selectedSiteId);

    return site ? this.siteLabel(site) : 'Site rattaché à votre profil';
  });

  protected readonly form = this.fb.group({
    siteId: this.fb.control<number | null>(null, Validators.required),
    terrainId: this.fb.control({ value: null as number | null, disabled: true }, Validators.required),
    date: this.fb.control<Date | null>(null, Validators.required),
    heure: this.fb.nonNullable.control({ value: '', disabled: true }, Validators.required),
    visibilite: this.fb.nonNullable.control<MatchVisibility>('PUBLIC', Validators.required)
  });

  ngOnInit(): void {
    this.loadProfile();
    this.loadSites();

    this.form.controls.siteId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((siteId) => {
        this.form.controls.terrainId.reset(null);
        this.resetSlots();
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

    this.form.controls.terrainId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.resetSelectedHour();
        this.refreshSlots();
      });

    this.form.controls.date.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.resetSelectedHour();
        this.refreshSlots();
      });
  }

  protected canSubmit(): boolean {
    return (
      this.form.valid &&
      this.form.controls.heure.enabled &&
      !this.isLoadingSites() &&
      !this.isLoadingTerrains() &&
      !this.isLoadingSlots() &&
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

    if (this.isLoadingSites() || this.isLoadingTerrains() || this.isLoadingSlots() || this.isSubmitting()) {
      return;
    }

    if (this.form.invalid) {
      return;
    }

    const payload = this.buildPayload();
    if (!payload) {
      this.globalErrorMessage.set(UI_MESSAGES.createMatch.incompleteForm);
      return;
    }

    this.isSubmitting.set(true);

    this.matchCreationService
      .createMatch(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (createdMatch) => {
          this.createdMatch.set(createdMatch);
          this.successMessage.set(UI_MESSAGES.createMatch.success);
          this.feedback.showSuccess(UI_MESSAGES.createMatch.success);
          this.form.controls.date.reset(null);
          this.resetSlots();
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

    if (this.isLoadingSlots()) {
      hints.push('Chargement des créneaux en cours.');
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

    if (
      this.form.controls.terrainId.value != null &&
      this.form.controls.date.value &&
      this.form.controls.heure.disabled &&
      !this.isLoadingSlots() &&
      !this.slotsMessage()
    ) {
      hints.push('Aucun créneau disponible.');
    } else if (this.form.controls.heure.disabled && !this.slotsMessage()) {
      hints.push('Sélectionnez un terrain et une date pour charger les créneaux.');
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

  protected formatSlotTime(value: string): string {
    return value.length >= 5 ? value.slice(0, 5) : value;
  }

  protected formatDuration(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;

    if (hours > 0 && remainingMinutes > 0) {
      return `${hours}h${remainingMinutes}`;
    }

    if (hours > 0) {
      return `${hours}h`;
    }

    return `${minutes} minutes`;
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
          this.applySiteProfileLock();
        },
        error: (error: HttpErrorResponse) => {
          this.globalErrorMessage.set(
            error.status === 0 ? 'Backend inaccessible.' : 'Impossible de charger les sites.'
          );
        }
      });
  }

  private loadProfile(): void {
    this.isLoadingProfile.set(true);

    this.meService
      .getMe()
      .pipe(finalize(() => this.isLoadingProfile.set(false)))
      .subscribe({
        next: (profile) => {
          this.currentProfile.set(profile);
          this.applySiteProfileLock();
        },
        error: () => {
          this.currentProfile.set(null);
        }
      });
  }

  private applySiteProfileLock(): void {
    const profile = this.currentProfile();
    const siteControl = this.form.controls.siteId;

    if (profile?.type !== 'SITE' || profile.siteId == null) {
      if (siteControl.disabled) {
        siteControl.enable({ emitEvent: false });
      }

      return;
    }

    if (siteControl.value !== profile.siteId) {
      siteControl.setValue(profile.siteId);
    }

    siteControl.disable({ emitEvent: false });
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
            this.resetSlots();
          }
        },
        error: (error: HttpErrorResponse) => {
          this.globalErrorMessage.set(
            error.status === 0 ? 'Backend inaccessible.' : 'Impossible de charger les terrains.'
          );
          this.terrains.set([]);
          this.form.controls.terrainId.disable();
          this.resetSlots();
        }
      });
  }

  private buildPayload(): CreateMatchPayload | null {
    const { terrainId, date, heure, visibilite } = this.form.getRawValue();
    const formattedDate = formatDateForApi(date);

    if (terrainId == null || !formattedDate || !heure) {
      return null;
    }

    return {
      terrainId,
      dateDebut: `${formattedDate}T${heure}:00`,
      visibilite
    };
  }

  private refreshSlots(): void {
    const { terrainId, date } = this.form.getRawValue();
    const formattedDate = formatDateForApi(date);

    if (terrainId == null || !formattedDate) {
      this.resetSlots();
      return;
    }

    this.availableSlots.set([]);
    this.slotsMessage.set('');
    this.form.controls.heure.disable();
    this.isLoadingSlots.set(true);

    const requestTerrainId = terrainId;
    const requestDate = formattedDate;

    this.matchCreationService
      .getCreneaux(requestTerrainId, requestDate)
      .pipe(finalize(() => this.isLoadingSlots.set(false)))
      .subscribe({
        next: (response) => {
          const current = this.form.getRawValue();
          if (current.terrainId !== requestTerrainId || formatDateForApi(current.date) !== requestDate) {
            return;
          }

          this.applySlotsResponse(response);
        },
        error: (error: HttpErrorResponse) => {
          const current = this.form.getRawValue();
          if (current.terrainId !== requestTerrainId || formatDateForApi(current.date) !== requestDate) {
            return;
          }

          this.availableSlots.set([]);
          this.form.controls.heure.disable();
          this.slotsMessage.set(
            error.status === 0 ? 'Backend inaccessible.' : 'Impossible de charger les créneaux.'
          );
        }
      });
  }

  private applySlotsResponse(response: MatchSlotsResponse): void {
    const slots = response.creneaux ?? [];

    this.availableSlots.set(slots);
    this.slotsMessage.set(response.message ?? '');
    this.slotScheduleInfo.set(this.toSlotScheduleInfo(response));

    if (slots.length > 0) {
      this.form.controls.heure.enable();
    } else {
      this.form.controls.heure.disable();
    }
  }

  private resetSelectedHour(): void {
    this.form.controls.heure.reset('');
  }

  private resetSlots(): void {
    this.availableSlots.set([]);
    this.slotsMessage.set('');
    this.slotScheduleInfo.set(null);
    this.isLoadingSlots.set(false);
    this.form.controls.heure.reset('');
    this.form.controls.heure.disable();
  }

  private toSlotScheduleInfo(response: MatchSlotsResponse): SlotScheduleInfo | null {
    if (
      response.annee == null ||
      !response.heureOuverture ||
      !response.heureFermeture ||
      response.dureeMatchMinutes == null ||
      response.bufferMinutes == null
    ) {
      return null;
    }

    return {
      annee: response.annee,
      heureOuverture: response.heureOuverture,
      heureFermeture: response.heureFermeture,
      dureeMatchMinutes: response.dureeMatchMinutes,
      bufferMinutes: response.bufferMinutes
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
      if (this.isPenaltyCreationError(apiError.message)) {
        this.globalErrorMessage.set(this.buildPenaltyCreationMessage(apiError.message));
        return;
      }

      this.globalErrorMessage.set(apiError.message);
      return;
    }

    if (error.status === 404) {
      this.globalErrorMessage.set('Terrain introuvable.');
      return;
    }

    this.globalErrorMessage.set('Impossible de créer le match.');
  }

  private isPenaltyCreationError(message: string): boolean {
    const normalizedMessage = this.normalizeForSearch(message);

    return normalizedMessage.includes('penalite') && normalizedMessage.includes('active');
  }

  private buildPenaltyCreationMessage(message: string): string {
    const date = this.extractIsoDateFromMessage(message);

    if (date) {
      return `Vous \u00eates actuellement p\u00e9nalis\u00e9 et ne pouvez pas cr\u00e9er de nouveau match jusqu\u2019au ${date} inclus.`;
    }

    return 'Vous \u00eates actuellement p\u00e9nalis\u00e9 et ne pouvez pas cr\u00e9er de nouveau match pour le moment.';
  }

  private extractIsoDateFromMessage(message: string): string | null {
    const match = message.match(/\b(\d{4})-(\d{2})-(\d{2})\b/);

    if (!match) {
      return null;
    }

    const [, year, month, day] = match;

    return `${day}/${month}/${year}`;
  }

  private normalizeForSearch(value: string): string {
    return value
      .replace(/\u00c3\u0192\u00c2\u00a9|\u00c3\u00a9/g, 'e')
      .replace(/\u00c3\u0192\u00c2\u00a8|\u00c3\u00a8/g, 'e')
      .replace(/\u00c3\u0192\u00c2\u00aa|\u00c3\u00aa/g, 'e')
      .replace(/\u00c3\u0192\u00c2\u00a0|\u00c3\u00a0/g, 'a')
      .replace(/\u00c3\u0192\u00c2\u00a7|\u00c3\u00a7/g, 'c')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
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

}
