import { CurrencyPipe, DatePipe, SlicePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { PlayerMatchRole } from '../../../core/matches/player-match-summary.models';
import { PlayerMatchesService } from '../../../core/matches/player-matches.service';
import { PublicMatch } from '../../../core/matches/public-match.models';
import { PublicMatchesService } from '../../../core/matches/public-matches.service';
import {
  BELGIAN_DATE_PROVIDERS,
  formatDateForApi,
  parseApiDateForPicker
} from '../../../shared/date/belgian-date-formats';
import {
  getSecondaryMatchBadge,
  getTemporalStatusClassName,
  getTemporalStatusLabel
} from '../../../shared/matches/match-status.utils';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { PageStateComponent } from '../../../shared/ui/page-state/page-state.component';
import { UI_MESSAGES } from '../../../shared/ui/ui-messages';

type PublicMatchDisplayFilter = 'TOUS' | 'DISPONIBLES' | 'COMPLETS' | 'DEJA_JOUES';

interface PublicMatchFilterOption {
  value: PublicMatchDisplayFilter;
  label: string;
}

interface PublicMatchCard {
  match: PublicMatch;
  temporalBadgeLabel: string;
  temporalBadgeClass: string;
  secondaryBadgeLabel: string;
  secondaryBadgeClass: string;
  detailLinkLabel: string;
}

const DISPLAY_FILTER_OPTIONS: PublicMatchFilterOption[] = [
  { value: 'TOUS', label: 'Tous' },
  { value: 'DISPONIBLES', label: 'Disponibles' },
  { value: 'COMPLETS', label: 'Complets' },
  { value: 'DEJA_JOUES', label: 'Déjà joués' }
];

@Component({
  selector: 'app-public-matches-page',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    SlicePipe,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    PageHeaderComponent,
    PageStateComponent
  ],
  providers: BELGIAN_DATE_PROVIDERS,
  templateUrl: './public-matches-page.component.html',
  styleUrl: './public-matches-page.component.css'
})
export class PublicMatchesPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly playerMatchesService = inject(PlayerMatchesService);
  private readonly publicMatchesService = inject(PublicMatchesService);

  protected readonly matches = signal<PublicMatch[]>([]);
  protected readonly playerMatchRoles = signal(new Map<number, PlayerMatchRole>());
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly displayFilterOptions = DISPLAY_FILTER_OPTIONS;
  protected readonly filters = signal({
    from: '',
    to: '',
    siteId: ''
  });
  protected readonly fromDate = computed(() => parseApiDateForPicker(this.filters().from));
  protected readonly toDate = computed(() => parseApiDateForPicker(this.filters().to));
  protected readonly selectedDisplayFilter = signal<PublicMatchDisplayFilter>('TOUS');
  protected readonly uiMessages = UI_MESSAGES;
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
  protected readonly filteredMatchCards = computed<PublicMatchCard[]>(() =>
    this.filteredMatches().map((match) => ({
      match,
      temporalBadgeLabel: getTemporalStatusLabel(match),
      temporalBadgeClass: getTemporalStatusClassName(match),
      secondaryBadgeLabel: this.getSecondaryBadge(match).label,
      secondaryBadgeClass: this.getSecondaryBadge(match).className,
      detailLinkLabel: this.getDetailLinkLabel(match)
    }))
  );
  protected readonly emptyStateMessage = computed(() => {
    if (this.matches().length === 0) {
      return UI_MESSAGES.publicMatches.empty;
    }

    switch (this.selectedDisplayFilter()) {
      case 'DISPONIBLES':
        return UI_MESSAGES.publicMatches.noAvailable;
      case 'COMPLETS':
        return UI_MESSAGES.publicMatches.noFull;
      case 'DEJA_JOUES':
        return UI_MESSAGES.publicMatches.noPast;
      case 'TOUS':
      default:
        return UI_MESSAGES.publicMatches.empty;
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

  protected updateDateFilter(field: 'from' | 'to', value: Date | null): void {
    this.updateFilter(field, formatDateForApi(value));
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

  private getSecondaryBadge(match: PublicMatch) {
    return getSecondaryMatchBadge(match, this.getPlayerRoleForMatch(match));
  }

  private getDetailLinkLabel(match: PublicMatch): string {
    if (this.getPlayerRoleForMatch(match) || !this.isMatchJoinable(match)) {
      return 'Voir le détail';
    }

    const montant = new Intl.NumberFormat('fr-BE', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(match.montantParJoueur);

    return `Voir le détail / rejoindre et payer ${montant} €`;
  }

  private isMatchJoinable(match: PublicMatch): boolean {
    if (this.getPlayerRoleForMatch(match)) {
      return false;
    }

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

  private getPlayerRoleForMatch(match: PublicMatch): PlayerMatchRole | null {
    return this.playerMatchRoles().get(match.id) ?? null;
  }

  private loadMatches(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    const filters = this.filters();
    const trimmedSiteId = filters.siteId.trim();
    const siteId = trimmedSiteId ? Number(trimmedSiteId) : null;

    const publicMatches$ = this.publicMatchesService.getPublicMatches({
        from: filters.from || undefined,
        to: filters.to || undefined,
        siteId: Number.isNaN(siteId) ? null : siteId
      });

    if (!this.authService.hasPlayerProfile()) {
      this.playerMatchRoles.set(new Map<number, PlayerMatchRole>());
      publicMatches$
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (matches) => {
          this.matches.set(matches);
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 0) {
            this.errorMessage.set(UI_MESSAGES.backendUnavailable);
            return;
          }

          this.errorMessage.set(UI_MESSAGES.publicMatches.loadError);
        }
      });
      return;
    }

    forkJoin({
      matches: publicMatches$,
      playerMatches: this.playerMatchesService.getMyMatches().pipe(catchError(() => of([])))
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ matches, playerMatches }) => {
          this.matches.set(matches);
          this.playerMatchRoles.set(
            new Map(playerMatches.map((match) => [match.id, match.roleJoueur]))
          );
        },
        error: (error: HttpErrorResponse) => {
          this.playerMatchRoles.set(new Map<number, PlayerMatchRole>());

          if (error.status === 0) {
            this.errorMessage.set(UI_MESSAGES.backendUnavailable);
            return;
          }

          this.errorMessage.set(UI_MESSAGES.publicMatches.loadError);
        }
      });
  }
}
