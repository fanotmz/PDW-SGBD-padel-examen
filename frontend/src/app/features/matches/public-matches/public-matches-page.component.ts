import { CommonModule, CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { PublicMatch } from '../../../core/matches/public-match.models';
import { PublicMatchesService } from '../../../core/matches/public-matches.service';

@Component({
  selector: 'app-public-matches-page',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  templateUrl: './public-matches-page.component.html',
  styleUrl: './public-matches-page.component.css'
})
export class PublicMatchesPageComponent implements OnInit {
  private readonly publicMatchesService = inject(PublicMatchesService);

  protected readonly matches = signal<PublicMatch[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly filters = signal({
    from: '2026-01-01',
    to: '2026-04-30',
    siteId: '1'
  });

  ngOnInit(): void {
    this.loadMatches();
  }

  protected updateFilter(field: 'from' | 'to' | 'siteId', value: string): void {
    this.filters.update((current) => ({
      ...current,
      [field]: value
    }));
  }

  protected submitFilters(event: Event): void {
    event.preventDefault();
    this.loadMatches();
  }

  private loadMatches(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    const filters = this.filters();
    const trimmedSiteId = filters.siteId.trim();
    const siteId = trimmedSiteId ? Number(trimmedSiteId) : null;

    this.publicMatchesService
      .getPublicMatches({
        from: filters.from || undefined,
        to: filters.to || undefined,
        siteId: Number.isNaN(siteId) ? null : siteId
      })
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

          this.errorMessage.set('Impossible de charger les matchs publics.');
        }
      });
  }
}
