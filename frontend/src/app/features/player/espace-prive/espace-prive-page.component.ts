import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin, Observable } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { MeProfile } from '../../../core/me/me.models';
import { MeService } from '../../../core/me/me.service';
import { PaiementService } from '../../../core/payments/paiement.service';
import { Regularisation } from '../../../core/regularisations/regularisation.models';
import { RegularisationsResponse } from '../../../core/regularisations/regularisation.models';
import { RegularisationService } from '../../../core/regularisations/regularisation.service';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-espace-prive-page',
  standalone: true,
  imports: [DatePipe, RouterLink, PageHeaderComponent],
  templateUrl: './espace-prive-page.component.html',
  styleUrl: './espace-prive-page.component.css'
})
export class EspacePrivePageComponent implements OnInit {
  private readonly authService = inject(AuthService);
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
  protected readonly pendingRegularisationConfirmationId = signal<number | null>(null);

  protected readonly hasDebt = computed(() => {
    const profile = this.profile();
    return profile != null && profile.solde > 0;
  });
  protected readonly hasActivePenalty = computed(() => {
    const penaliteJusqua = this.profile()?.penaliteJusqua;

    if (!penaliteJusqua) {
      return false;
    }

    return new Date(penaliteJusqua).getTime() > Date.now();
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
      return `Vous avez actuellement ${this.formatAmount(profile.solde)} à régulariser.`;
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
    }).format(amount) + ' €';
  }

  protected isPaying(participationId: number): boolean {
    return this.payingParticipationId() === participationId;
  }

  protected isRegularisationConfirmationOpen(regularisation: Regularisation): boolean {
    return this.pendingRegularisationConfirmationId() === regularisation.participationId;
  }

  protected isLateCancellationRegularisation(regularisation: Regularisation): boolean {
    return regularisation.origineType === 'ANNULATION_TARDIVE_ORGANISATEUR';
  }

  protected getPaymentButtonLabel(regularisation: Regularisation): string {
    if (this.isPaying(regularisation.participationId)) {
      return 'Paiement en cours...';
    }

    if (this.isLateCancellationRegularisation(regularisation)) {
      return `Régulariser ${this.formatAmount(regularisation.montantRestant)}`;
    }

    return 'Payer';
  }

  protected requestRegularisationPayment(regularisation: Regularisation): void {
    if (!regularisation.payable || this.isPaying(regularisation.participationId)) {
      return;
    }

    this.paymentSuccessMessage.set('');
    this.paymentErrorMessage.set('');

    if (this.isLateCancellationRegularisation(regularisation)) {
      this.pendingRegularisationConfirmationId.set(regularisation.participationId);
      return;
    }

    this.payerRegularisation(regularisation);
  }

  protected closeRegularisationConfirmation(): void {
    if (this.payingParticipationId() == null) {
      this.pendingRegularisationConfirmationId.set(null);
    }
  }

  protected payerRegularisation(regularisation: Regularisation): void {
    if (!regularisation.payable || this.isPaying(regularisation.participationId)) {
      return;
    }

    this.paymentSuccessMessage.set('');
    this.paymentErrorMessage.set('');
    this.pendingRegularisationConfirmationId.set(null);
    this.payingParticipationId.set(regularisation.participationId);

    const payment$: Observable<unknown> = this.isLateCancellationRegularisation(regularisation)
      ? this.regularisationService.payLateCancellationRegularisation(
        regularisation.participationId,
        regularisation.montantRestant
      )
      : this.paiementService.payerParticipation(regularisation.participationId, regularisation.montantRestant);

    payment$
      .pipe(finalize(() => this.payingParticipationId.set(null)))
      .subscribe({
        next: () => {
          this.paymentSuccessMessage.set('Paiement enregistré avec succès.');
          if (this.isLateCancellationRegularisation(regularisation)) {
            this.paymentSuccessMessage.set('Régularisation enregistrée avec succès. La dette financière est réduite, mais la pénalité de réservation reste active jusqu’à son terme.');
          }
          this.reloadFinancialData();
        },
        error: (error: HttpErrorResponse) => {
          this.paymentErrorMessage.set(this.getPaymentErrorMessage(error));
        }
      });
  }

  private loadPageData(): void {
    if (this.authService.isAdmin() && !this.authService.hasPlayerProfile()) {
      this.isLoading.set(false);
      this.errorMessage.set('Cet espace est réservé aux joueurs.');
      return;
    }

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

          if (error.status === 403 && this.authService.isAdmin() && !this.authService.hasPlayerProfile()) {
            this.errorMessage.set('Cet espace est réservé aux joueurs.');
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
