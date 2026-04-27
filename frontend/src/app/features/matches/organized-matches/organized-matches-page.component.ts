import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { OrganizedMatchSummary } from '../../../core/matches/organized-match-summary.models';
import { OrganizedMatchesService } from '../../../core/matches/organized-matches.service';

@Component({
  selector: 'app-organized-matches-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './organized-matches-page.component.html',
  styleUrl: './organized-matches-page.component.css'
})
export class OrganizedMatchesPageComponent implements OnInit {
  private readonly organizedMatchesService = inject(OrganizedMatchesService);

  protected readonly matches = signal<OrganizedMatchSummary[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  ngOnInit(): void {
    this.loadMatches();
  }

  private loadMatches(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.organizedMatchesService
      .getMyOrganizedMatches()
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

          this.errorMessage.set('Impossible de charger vos matchs organisés.');
        }
      });
  }
}
