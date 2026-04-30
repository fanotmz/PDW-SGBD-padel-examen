import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { PublicMatch } from '../../../core/matches/public-match.models';
import { PublicMatchesService } from '../../../core/matches/public-matches.service';

type PublicMatchDisplayFilter = 'TOUS' | 'DISPONIBLES' | 'COMPLETS' | 'DEJA_JOUES';

interface PublicMatchFilterOption {
  value: PublicMatchDisplayFilter;
  label: string;
}

const DISPLAY_FILTER_OPTIONS: PublicMatchFilterOption[] = [
  { value: 'TOUS', label: 'Tous' },
  { value: 'DISPONIBLES', label: 'Disponibles' },
  { value: 'COMPLETS', label: 'Complets' },
  { value: 'DEJA_JOUES', label: 'D\u00e9j\u00e0 jou\u00e9s' }
];

@Component({
  selector: 'app-public-matches-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './public-matches-page.component.html',
  styleUrl: './public-matches-page.component.css'
})
export class PublicMatchesPageComponent implements OnInit {
  private readonly publicMatchesService = inject(PublicMatchesService);

  protected readonly matches = signal<PublicMatch[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly displayFilterOptions = DISPLAY_FILTER_OPTIONS;
  protected readonly filters = signal({
    from: '2026-01-01',
    to: '2026-04-30',
    siteId: '1'
  });
  protected readonly selectedDisplayFilter = signal<PublicMatchDisplayFilter>('TOUS');
  protected readonly filteredMatches = computed(() => {
    const selectedFilter = this.selectedDisplayFilter();

    return this.matches().filter((match) => {
      switch (selectedFilter) {
        case 'DISPONIBLES':
          return this.isMatchJoinable(match);
        case 'COMPLETS':
          return match.complet;
        case 'DEJA_JOUES':
          return this.isPastMatch(match);
        case 'TOUS':
        default:
          return true;
      }
    });
  });
  protected readonly emptyStateMessage = computed(() => {
    if (this.matches().length === 0) {
      return 'Aucun match public disponible pour le moment.';
    }

    switch (this.selectedDisplayFilter()) {
      case 'DISPONIBLES':
        return 'Aucun match public disponible \u00e0 afficher pour ce filtre.';
      case 'COMPLETS':
        return 'Aucun match complet \u00e0 afficher pour ce filtre.';
      case 'DEJA_JOUES':
        return 'Aucun match d\u00e9j\u00e0 jou\u00e9 \u00e0 afficher pour ce filtre.';
      case 'TOUS':
      default:
        return 'Aucun match public disponible pour le moment.';
    }
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

  protected selectDisplayFilter(filter: PublicMatchDisplayFilter): void {
    this.selectedDisplayFilter.set(filter);
  }

  protected isFilterSelected(filter: PublicMatchDisplayFilter): boolean {
    return this.selectedDisplayFilter() === filter;
  }

  protected getDetailLinkLabel(match: PublicMatch): string {
    if (!this.isMatchJoinable(match)) {
      return 'Voir le d\u00e9tail';
    }

    const montant = new Intl.NumberFormat('fr-BE', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(match.montantParJoueur);

    return `Voir le d\u00e9tail / rejoindre et payer ${montant} \u20ac`;
  }

  protected getMatchBadgeLabel(match: PublicMatch): string {
    if (match.statut === 'ANNULE') {
      return 'Annul\u00e9';
    }

    if (this.isPastMatch(match)) {
      return 'D\u00e9j\u00e0 jou\u00e9';
    }

    return match.complet ? 'Complet' : 'Places disponibles';
  }

  protected getMatchBadgeClass(match: PublicMatch): string {
    if (match.statut === 'ANNULE') {
      return 'is-cancelled';
    }

    if (this.isPastMatch(match)) {
      return 'is-past';
    }

    return match.complet ? 'is-full' : 'is-open';
  }

  private isMatchJoinable(match: PublicMatch): boolean {
    if (match.complet || this.isPastMatch(match)) {
      return false;
    }

    if (match.statut) {
      return match.statut === 'PLANIFIE';
    }

    return true;
  }

  private isPastMatch(match: PublicMatch): boolean {
    if (match.statutTemporel) {
      return match.statutTemporel === 'PASSE';
    }

    return this.getMatchStartDate(match).getTime() < Date.now();
  }

  private getMatchStartDate(match: PublicMatch): Date {
    const [year, month, day] = match.dateDebut.split('-').map((value) => Number(value));
    const [hours = 0, minutes = 0] = match.heureDebut.split(':').map((value) => Number(value));

    return new Date(year, month - 1, day, hours, minutes);
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
