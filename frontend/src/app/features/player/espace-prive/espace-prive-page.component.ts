import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { MeProfile } from '../../../core/me/me.models';
import { MeService } from '../../../core/me/me.service';
import { PaiementService } from '../../../core/payments/paiement.service';
import { Regularisation } from '../../../core/regularisations/regularisation.models';
import { RegularisationsResponse } from '../../../core/regularisations/regularisation.models';
import { RegularisationService } from '../../../core/regularisations/regularisation.service';

@Component({
  selector: 'app-espace-prive-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './espace-prive-page.component.html',
  styleUrl: './espace-prive-page.component.css'
})
export class EspacePrivePageComponent implements OnInit {
  private readonly meService = inject(MeService);
  private readonly regularisationService = inject(RegularisationService);
  private readonly paiementService = inject(PaiementService);

  protected readonly profile = signal<MeProfile | null>(null);
  protected readonly regularisations = signal<RegularisationsResponse>({
    totalTracable: 0,
    items: []
  });
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly paymentSuccessMessage = signal('');
  protected readonly paymentErrorMessage = signal('');
  protected readonly payingParticipationId = signal<number | null>(null);

  protected readonly hasDebt = computed(() => {
    const profile = this.profile();
    return profile != null && profile.solde > 0;
  });
  protected readonly regularisationItems = computed(() => this.regularisations().items);
  protected readonly totalTracable = computed(() => this.regularisations().totalTracable);
  protected readonly hasTraceableGap = computed(() => {
    const profile = this.profile();
    return profile != null && this.totalTracable() < profile.solde;
  });
  protected readonly hasRegularisations = computed(() => this.regularisationItems().length > 0);

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

  protected getRoleLabel(role: 'ORGANISATEUR' | 'PARTICIPANT'): string {
    return role === 'ORGANISATEUR' ? 'Organisateur' : 'Participant';
  }

  protected formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-BE', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount) + ' \u20ac';
  }

  protected isPaying(participationId: number): boolean {
    return this.payingParticipationId() === participationId;
  }

  protected payerRegularisation(regularisation: Regularisation): void {
    if (!regularisation.payable || this.isPaying(regularisation.participationId)) {
      return;
    }

    this.paymentSuccessMessage.set('');
    this.paymentErrorMessage.set('');
    this.payingParticipationId.set(regularisation.participationId);

    this.paiementService
      .payerParticipation(regularisation.participationId, regularisation.montantRestant)
      .pipe(finalize(() => this.payingParticipationId.set(null)))
      .subscribe({
        next: () => {
          this.paymentSuccessMessage.set('Paiement enregistré avec succès.');
          this.reloadFinancialData();
        },
        error: (error: HttpErrorResponse) => {
          this.paymentErrorMessage.set(this.getPaymentErrorMessage(error));
        }
      });
  }

  private loadPageData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.paymentSuccessMessage.set('');
    this.paymentErrorMessage.set('');

    forkJoin({
      profile: this.meService.getMe(),
      regularisations: this.regularisationService.getMyRegularisations()
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ profile, regularisations }) => {
          this.profile.set(profile);
          this.regularisations.set(regularisations);
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

  private reloadFinancialData(): void {
    forkJoin({
      profile: this.meService.getMe(),
      regularisations: this.regularisationService.getMyRegularisations()
    }).subscribe({
      next: ({ profile, regularisations }) => {
        this.profile.set(profile);
        this.regularisations.set(regularisations);
      },
      error: () => {
        this.paymentErrorMessage.set('Le paiement a été enregistré, mais la mise à jour de l’espace a échoué.');
      }
    });
  }

  private getPaymentErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Backend inaccessible.';
    }

    const backendMessage =
      typeof error.error?.message === 'string'
        ? error.error.message
        : typeof error.error?.error === 'string'
          ? error.error.error
          : '';

    return backendMessage || 'Impossible d’enregistrer le paiement.';
  }
}
