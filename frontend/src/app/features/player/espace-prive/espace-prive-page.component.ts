import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { PlayerMatchSummary } from '../../../core/matches/player-match-summary.models';
import { PlayerMatchesService } from '../../../core/matches/player-matches.service';
import { MeProfile } from '../../../core/me/me.models';
import { MeService } from '../../../core/me/me.service';

@Component({
  selector: 'app-espace-prive-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './espace-prive-page.component.html',
  styleUrl: './espace-prive-page.component.css'
})
export class EspacePrivePageComponent implements OnInit {
  private readonly meService = inject(MeService);
  private readonly playerMatchesService = inject(PlayerMatchesService);

  protected readonly profile = signal<MeProfile | null>(null);
  protected readonly matches = signal<PlayerMatchSummary[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  protected readonly hasDebt = computed(() => {
    const profile = this.profile();
    return profile != null && profile.solde > 0;
  });
  protected readonly unpaidMatches = computed(() =>
    this.matches().filter((match) => match.montantRestantJoueur > 0)
  );

  protected readonly debtMessage = computed(() => {
    const profile = this.profile();
    if (profile == null) {
      return '';
    }

    if (profile.solde > 0) {
      return `Vous avez actuellement ${this.formatAmount(profile.solde)} \u00e0 r\u00e9gulariser.`;
    }

    return 'Aucune dette en cours.';
  });

  ngOnInit(): void {
    this.loadPageData();
  }

  protected getTypeLabel(type: MeProfile['type']): string {
    switch (type) {
      case 'GLOBAL':
        return 'Global';
      case 'SITE':
        return 'Site';
      case 'LIBRE':
      default:
        return 'Libre';
    }
  }

  protected formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-BE', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount) + ' \u20ac';
  }

  private loadPageData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    forkJoin({
      profile: this.meService.getMe(),
      matches: this.playerMatchesService.getMyMatches()
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ profile, matches }) => {
          this.profile.set(profile);
          this.matches.set(matches);
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 0) {
            this.errorMessage.set('Backend inaccessible.');
            return;
          }

          this.errorMessage.set('Impossible de charger votre espace.');
        }
      });
  }
}
