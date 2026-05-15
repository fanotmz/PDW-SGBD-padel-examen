import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminMatchScope, AdminMatchStateFilter, AdminSiteMatchSummaryResponse } from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-site-matches-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './admin-site-matches-page.component.html',
  styleUrl: './admin-site-matches-page.component.css'
})
export class AdminSiteMatchesPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly adminService = inject(AdminService);

  protected readonly siteId = signal<number | null>(null);
  protected readonly matches = signal<AdminSiteMatchSummaryResponse[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly from = signal('');
  protected readonly to = signal('');
  protected readonly etat = signal<AdminMatchStateFilter>('ALL');
  protected readonly visibilite = signal('');
  protected readonly pageTitle = computed(() => 'Matchs du site');

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('siteId');
    const id = idParam ? Number(idParam) : Number.NaN;

    if (Number.isNaN(id) || id <= 0) {
      this.isLoading.set(false);
      this.errorMessage.set('Identifiant de site invalide.');
      return;
    }

    this.siteId.set(id);
    this.loadMatches(id);
  }

  protected hasNoMatches(): boolean {
    return !this.isLoading() && !this.errorMessage() && this.matches().length === 0;
  }

  protected trackMatch(match: AdminSiteMatchSummaryResponse): number {
    return match.id;
  }

  protected onFromChange(value: string): void {
    this.from.set(value);
  }

  protected onToChange(value: string): void {
    this.to.set(value);
  }

  protected onStateChange(value: string): void {
    this.etat.set(this.toStateFilter(value));
  }

  protected onVisibilityChange(value: string): void {
    this.visibilite.set(value);
  }

  protected applyFilters(): void {
    const siteId = this.siteId();
    if (siteId == null) {
      return;
    }

    this.loadMatches(siteId);
  }

  protected resetFilters(): void {
    this.from.set('');
    this.to.set('');
    this.etat.set('ALL');
    this.visibilite.set('');
    this.applyFilters();
  }

  protected formatTime(value: string): string {
    if (!value) {
      return 'Non renseigné';
    }

    return value.length >= 5 ? value.slice(0, 5) : value;
  }

  protected getVisibilityLabel(visibilite: string): string {
    if (visibilite === 'PUBLIC') {
      return 'Public';
    }

    if (visibilite === 'PRIVE') {
      return 'Privé';
    }

    return visibilite;
  }

  protected getMatchStateLabel(match: AdminSiteMatchSummaryResponse): string {
    if (match.statut === 'ANNULE') {
      return 'Annulé';
    }

    if (this.isPastMatch(match)) {
      return 'Déjà joué';
    }

    return 'À venir';
  }

  protected isCancelled(match: AdminSiteMatchSummaryResponse): boolean {
    return match.statut === 'ANNULE';
  }

  protected isPlayed(match: AdminSiteMatchSummaryResponse): boolean {
    return !this.isCancelled(match) && this.isPastMatch(match);
  }

  protected isUpcoming(match: AdminSiteMatchSummaryResponse): boolean {
    return !this.isCancelled(match) && !this.isPastMatch(match);
  }

  protected goBackToSites(): void {
    this.router.navigateByUrl('/admin/sites');
  }

  private loadMatches(siteId: number): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    const matchFilters = this.toBackendMatchFilters();

    this.adminService
      .getSiteMatches(siteId, {
        scope: matchFilters.scope,
        from: this.from(),
        to: this.to(),
        statut: matchFilters.statut,
        visibilite: this.visibilite()
      })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (matches) => {
          this.matches.set(matches);
        },
        error: (error: HttpErrorResponse) => {
          this.matches.set([]);
          this.errorMessage.set(this.getErrorMessage(error));
        }
      });
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Backend inaccessible.';
    }

    if (error.status === 401) {
      return 'Authentification requise.';
    }

    if (error.status === 403) {
      return 'Accès administrateur refusé pour ce site.';
    }

    if (error.status === 404) {
      return 'Site introuvable.';
    }

    return 'Impossible de charger les matchs du site.';
  }

  private toStateFilter(value: string): AdminMatchStateFilter {
    if (value === 'UPCOMING' || value === 'PLAYED' || value === 'CANCELLED') {
      return value;
    }

    return 'ALL';
  }

  private toBackendMatchFilters(): { scope: AdminMatchScope; statut: string } {
    switch (this.etat()) {
      case 'UPCOMING':
        return { scope: 'UPCOMING_PLANNED', statut: '' };
      case 'PLAYED':
        return { scope: 'HISTORY', statut: 'PLANIFIE' };
      case 'CANCELLED':
        return { scope: 'ALL', statut: 'ANNULE' };
      default:
        return { scope: 'ALL', statut: '' };
    }
  }

  private isPastMatch(match: AdminSiteMatchSummaryResponse): boolean {
    const matchDate = this.toLocalMatchDate(match);
    if (matchDate == null) {
      return match.passe;
    }

    return matchDate.getTime() < Date.now();
  }

  private toLocalMatchDate(match: AdminSiteMatchSummaryResponse): Date | null {
    const dateParts = match.dateDebut.split('-').map(Number);
    const timeParts = (match.heureDebut || '00:00:00').split(':').map(Number);

    if (dateParts.length < 3 || dateParts.some(Number.isNaN)) {
      return null;
    }

    const [year, month, day] = dateParts;
    const [hour = 0, minute = 0, second = 0] = timeParts;
    return new Date(year, month - 1, day, hour, minute, second);
  }
}
