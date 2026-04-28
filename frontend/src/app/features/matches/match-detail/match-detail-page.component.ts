import { CommonModule, CurrencyPipe, DatePipe, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatchParticipationService } from '../../../core/matches/match-participation.service';
import { MatchDetail } from '../../../core/matches/match-detail.models';
import { MatchDetailService } from '../../../core/matches/match-detail.service';

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

  protected readonly match = signal<MatchDetail | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly isJoining = signal(false);
  protected readonly joinSuccessMessage = signal('');
  protected readonly joinErrorMessage = signal('');
  protected readonly joinErrorDetails = signal<Record<string, string> | null>(null);
  protected readonly canShowJoinButton = computed(() => {
    const detail = this.match();

    return !!detail
      && detail.visibilite === 'PUBLIC'
      && detail.statut === 'PLANIFIE'
      && detail.complet === false;
  });

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
          this.joinSuccessMessage.set('Vous avez rejoint le match avec succès.');
          this.loadMatchDetail(detail.id);
        },
        error: (error: HttpErrorResponse) => {
          this.handleJoinError(error);
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

    this.matchDetailService
      .getMatchDetail(id)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (match) => {
          this.match.set(match);
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
      this.errorMessage.set(apiError.message ?? 'Accès refusé à ce match.');
      return;
    }

    if (error.status === 404) {
      this.errorMessage.set('Match introuvable.');
      return;
    }

    this.errorMessage.set(apiError.message ?? 'Impossible de charger le détail du match.');
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
