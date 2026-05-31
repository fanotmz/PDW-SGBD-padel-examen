import { DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { OrganizedMatchSummary } from '../../../core/matches/organized-match-summary.models';
import { OrganizedMatchesService } from '../../../core/matches/organized-matches.service';
import {
  getMatchStatusClassMap,
  getMatchStatusLabel,
  getSecondaryMatchBadge
} from '../../../shared/matches/match-status.utils';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { PageStateComponent } from '../../../shared/ui/page-state/page-state.component';

@Component({
  selector: 'app-organized-matches-page',
  standalone: true,
  imports: [NgClass, RouterLink, DatePipe, PageHeaderComponent, PageStateComponent],
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

  protected getDisplayStatusLabel(match: OrganizedMatchSummary): string {
    return getMatchStatusLabel(match);
  }

  protected getStatusClassMap(match: OrganizedMatchSummary): Record<string, boolean> {
    return getMatchStatusClassMap(match);
  }

  protected getOrganizerBadgeLabel(match: OrganizedMatchSummary): string {
    return getSecondaryMatchBadge(match, 'ORGANISATEUR').label;
  }

  protected getOrganizerBadgeClass(match: OrganizedMatchSummary): string {
    return getSecondaryMatchBadge(match, 'ORGANISATEUR').className;
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
