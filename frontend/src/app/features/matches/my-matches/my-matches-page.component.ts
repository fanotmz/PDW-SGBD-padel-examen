import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { PlayerMatchSummary } from '../../../core/matches/player-match-summary.models';
import { PlayerMatchesService } from '../../../core/matches/player-matches.service';

@Component({
  selector: 'app-my-matches-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
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
