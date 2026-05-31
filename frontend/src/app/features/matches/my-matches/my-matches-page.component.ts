import { DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { PlayerMatchSummary } from '../../../core/matches/player-match-summary.models';
import { PlayerMatchesService } from '../../../core/matches/player-matches.service';
import {
  getMatchStatusClassMap,
  getMatchStatusLabel,
  getSecondaryMatchBadge
} from '../../../shared/matches/match-status.utils';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { PageStateComponent } from '../../../shared/ui/page-state/page-state.component';

@Component({
  selector: 'app-my-matches-page',
  standalone: true,
  imports: [NgClass, RouterLink, DatePipe, PageHeaderComponent, PageStateComponent],
  templateUrl: './my-matches-page.component.html',
  styleUrl: './my-matches-page.component.css'
})
export class MyMatchesPageComponent implements OnInit {
  private readonly playerMatchesService = inject(PlayerMatchesService);

  protected readonly matches = signal<PlayerMatchSummary[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  ngOnInit(): void {
    this.loadMatches();
  }

  protected getDisplayStatusLabel(match: PlayerMatchSummary): string {
    return getMatchStatusLabel(match);
  }

  protected getStatusClassMap(match: PlayerMatchSummary): Record<string, boolean> {
    return getMatchStatusClassMap(match);
  }

  protected getUserBadgeLabel(match: PlayerMatchSummary): string {
    return getSecondaryMatchBadge(match, match.roleJoueur).label;
  }

  protected getUserBadgeClass(match: PlayerMatchSummary): string {
    return getSecondaryMatchBadge(match, match.roleJoueur).className;
  }

  private loadMatches(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.playerMatchesService
      .getMyMatches()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (matches) => {
          this.matches.set(matches);
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 0) {
            this.errorMessage.set('Backend inaccessible.');
            return;
          }

          this.errorMessage.set('Impossible de charger vos matchs.');
        }
      });
  }
}
