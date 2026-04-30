import { CommonModule, CurrencyPipe, DatePipe, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { MatchParticipationService } from '../../../core/matches/match-participation.service';
import { MatchDetail } from '../../../core/matches/match-detail.models';
import { MatchDetailService } from '../../../core/matches/match-detail.service';
import { MeProfile } from '../../../core/me/me.models';
import { MeService } from '../../../core/me/me.service';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string> | null;
}

@Component({
  selector: 'app-match-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, CurrencyPipe],
  templateUrl: './match-detail-page.component.html',
  styleUrl: './match-detail-page.component.css'
})
export class MatchDetailPageComponent implements OnInit {
  private readonly location = inject(Location);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly matchDetailService = inject(MatchDetailService);
  private readonly matchParticipationService = inject(MatchParticipationService);
  private readonly meService = inject(MeService);

  protected readonly match = signal<MatchDetail | null>(null);
  protected readonly currentProfile = signal<MeProfile | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly isJoining = signal(false);
  protected readonly joinSuccessMessage = signal('');
  protected readonly joinErrorMessage = signal('');
  protected readonly joinErrorDetails = signal<Record<string, string> | null>(null);
  protected readonly privatePlayerMatricule = signal('');
  protected readonly isAddingPrivatePlayer = signal(false);
  protected readonly addPrivateSuccessMessage = signal('');
  protected readonly addPrivateErrorMessage = signal('');
  protected readonly addPrivateErrorDetails = signal<Record<string, string> | null>(null);
  protected readonly canShowJoinButton = computed(() => {
    const detail = this.match();
    const currentProfile = this.currentProfile();

    return !!detail
      && !!currentProfile
      && detail.visibilite === 'PUBLIC'
      && detail.statut === 'PLANIFIE'
      && detail.complet === false
      && !detail.participants.some((participant) => participant.matricule === currentProfile.matricule);
  });
  protected readonly isAlreadyParticipant = computed(() => {
    const detail = this.match();
    const currentProfile = this.currentProfile();

    return !!detail
      && !!currentProfile
      && detail.participants.some((participant) => participant.matricule === currentProfile.matricule);
  });
  protected readonly canShowPrivateAddForm = computed(() => this.match()?.peutAjouterJoueurPrive === true);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : Number.NaN;

    if (Number.isNaN(id) || id <= 0) {
      this.isLoading.set(false);
      this.errorMessage.set('Identifiant de match invalide.');
      return;
    }

    this.loadMatchDetail(id);
  }

  protected goBack(): void {
    if (window.history.length > 1) {
      this.location.back();
      return;
    }

    this.router.navigateByUrl('/matchs');
  }

  protected joinPublicMatch(): void {
    const detail = this.match();
    if (!detail || !this.canShowJoinButton() || this.isJoining()) {
      return;
    }

    this.joinSuccessMessage.set('');
    this.joinErrorMessage.set('');
    this.joinErrorDetails.set(null);
    this.isJoining.set(true);

    this.matchParticipationService
      .rejoindreMatchPublic(detail.id)
      .pipe(finalize(() => this.isJoining.set(false)))
      .subscribe({
        next: () => {
          this.joinSuccessMessage.set('Vous avez rejoint le match et pay\u00e9 votre participation avec succ\u00e8s.');
          this.loadMatchDetail(detail.id);
        },
        error: (error: HttpErrorResponse) => {
          this.handleJoinError(error);
        }
      });
  }

  protected updatePrivatePlayerMatricule(event: Event): void {
    const target = event.target as HTMLInputElement | null;
    this.privatePlayerMatricule.set(target?.value ?? '');
  }

  protected addPrivatePlayer(): void {
    const detail = this.match();
    const matricule = this.privatePlayerMatricule().trim();

    if (!detail || !this.canShowPrivateAddForm() || this.isAddingPrivatePlayer()) {
      return;
    }

    this.addPrivateSuccessMessage.set('');
    this.addPrivateErrorMessage.set('');
    this.addPrivateErrorDetails.set(null);

    if (!matricule) {
      this.addPrivateErrorMessage.set('Le matricule du joueur est obligatoire.');
      return;
    }

    this.isAddingPrivatePlayer.set(true);

    this.matchParticipationService
      .ajouterJoueurPrive(detail.id, matricule)
      .pipe(finalize(() => this.isAddingPrivatePlayer.set(false)))
      .subscribe({
        next: () => {
          this.privatePlayerMatricule.set('');
          this.addPrivateSuccessMessage.set('Le joueur a \u00e9t\u00e9 ajout\u00e9 avec succ\u00e8s.');
          this.loadMatchDetail(detail.id);
        },
        error: (error: HttpErrorResponse) => {
          this.handleAddPrivatePlayerError(error);
        }
      });
  }

  protected getDisplayStatusLabel(detail: MatchDetail): string {
    if (detail.statut === 'ANNULE') {
      return 'Annul\u00e9';
    }

    const matchDay = this.getDayTimestamp(detail.dateDebut);
    const today = this.getCurrentDayTimestamp();

    if (matchDay < today) {
      return 'D\u00e9j\u00e0 jou\u00e9';
    }

    if (matchDay > today) {
      return '\u00c0 venir';
    }

    return 'Aujourd\u2019hui';
  }

  protected isCancelled(detail: MatchDetail): boolean {
    return detail.statut === 'ANNULE';
  }

  private loadMatchDetail(id: number): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    forkJoin({
      match: this.matchDetailService.getMatchDetail(id),
      profile: this.meService.getMe()
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ match, profile }) => {
          this.match.set(match);
          this.currentProfile.set(profile);
        },
        error: (error: HttpErrorResponse) => {
          this.handleLoadError(error);
        }
      });
  }

  private handleLoadError(error: HttpErrorResponse): void {
    const apiError = this.normalizeApiErrorBody(error.error);

    if (error.status === 0) {
      this.errorMessage.set('Backend inaccessible.');
      return;
    }

    if (error.status === 403) {
      this.errorMessage.set(apiError.message ?? 'Acc\u00e8s refus\u00e9 \u00e0 ce match.');
      return;
    }

    if (error.status === 404) {
      this.errorMessage.set('Match introuvable.');
      return;
    }

    this.errorMessage.set(apiError.message ?? 'Impossible de charger le d\u00e9tail du match.');
  }

  private handleJoinError(error: HttpErrorResponse): void {
    const apiError = this.normalizeApiErrorBody(error.error);

    if (error.status === 0) {
      this.joinErrorMessage.set('Backend inaccessible.');
      return;
    }

    this.joinErrorMessage.set(apiError.message ?? 'Impossible de rejoindre le match.');
    this.joinErrorDetails.set(apiError.details ?? null);
  }

  private handleAddPrivatePlayerError(error: HttpErrorResponse): void {
    const apiError = this.normalizeApiErrorBody(error.error);

    if (error.status === 0) {
      this.addPrivateErrorMessage.set('Backend inaccessible.');
      return;
    }

    this.addPrivateErrorMessage.set(apiError.message ?? "Impossible d'ajouter le joueur.");
    this.addPrivateErrorDetails.set(apiError.details ?? null);
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

  private getCurrentDayTimestamp(): number {
    const now = new Date();

    return new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  }

  private getDayTimestamp(dateValue: string): number {
    const [year, month, day] = dateValue.split('-').map((value) => Number(value));

    return new Date(year, month - 1, day).getTime();
  }
}
