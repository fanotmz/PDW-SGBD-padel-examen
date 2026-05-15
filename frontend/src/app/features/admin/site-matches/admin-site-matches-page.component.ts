import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminSiteMatchSummaryResponse } from '../../../core/admin/admin.models';
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
  protected readonly pageTitle = computed(() => 'Matchs planifiés du site');

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

  protected formatTime(value: string): string {
    if (!value) {
      return 'Non renseigné';
    }

    return value.length >= 5 ? value.slice(0, 5) : value;
  }

  protected getStatusLabel(statut: string): string {
    if (statut === 'PLANIFIE') {
      return 'Planifié';
    }

    if (statut === 'ANNULE') {
      return 'Annulé';
    }

    return statut;
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

  protected goBackToSites(): void {
    this.router.navigateByUrl('/admin/sites');
  }

  private loadMatches(siteId: number): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService
      .getSiteMatches(siteId)
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
}
