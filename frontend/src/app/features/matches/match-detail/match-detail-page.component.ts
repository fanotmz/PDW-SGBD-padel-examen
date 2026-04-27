import { CommonModule, CurrencyPipe, DatePipe, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatchDetail } from '../../../core/matches/match-detail.models';
import { MatchDetailService } from '../../../core/matches/match-detail.service';

interface ApiErrorBody {
  message?: string;
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

  protected readonly match = signal<MatchDetail | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

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
}
