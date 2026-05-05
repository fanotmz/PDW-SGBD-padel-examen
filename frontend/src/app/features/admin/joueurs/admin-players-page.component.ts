import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import {
  AdminInfoResponse,
  AdminPlayerListItem,
  AdminPlayerResponse,
  AdminSitePlayerResponse,
  RegistrationSubscriptionType
} from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';

@Component({
  selector: 'app-admin-players-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-players-page.component.html',
  styleUrl: './admin-players-page.component.css'
})
export class AdminPlayersPageComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly sitesService = inject(SitesService);
  private readonly currencyFormatter = new Intl.NumberFormat('fr-BE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });

  protected readonly adminInfo = signal<AdminInfoResponse | null>(null);
  protected readonly players = signal<AdminPlayerListItem[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  protected readonly isEmpty = computed(() => !this.isLoading() && !this.errorMessage() && this.players().length === 0);

  protected readonly playersCountMessage = computed(() => {
    const count = this.players().length;

    if (count === 0) {
      return 'Aucun joueur accessible';
    }

    if (count === 1) {
      return '1 joueur accessible';
    }

    return `${count} joueurs accessibles`;
  });

  ngOnInit(): void {
    this.loadPlayers();
  }

  protected getTypeLabel(type: RegistrationSubscriptionType): string {
    switch (type) {
      case 'GLOBAL':
        return 'Membre global';
      case 'SITE':
        return "Membre d'un site";
      case 'LIBRE':
      default:
        return 'Membre libre';
    }
  }

  protected hasSite(player: AdminPlayerListItem): boolean {
    return player.siteNom != null;
  }

  protected getBalanceLabel(player: AdminPlayerListItem): string {
    return player.solde != null && player.solde > 0 ? 'Dette' : 'Solde';
  }

  protected formatBalance(solde: number | null): string {
    if (solde == null) {
      return 'Indisponible';
    }

    return `${this.currencyFormatter.format(solde)} \u20ac`;
  }

  private loadPlayers(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService
      .getInfo()
      .pipe(
        switchMap((adminInfo) => {
          this.adminInfo.set(adminInfo);

          if (adminInfo.adminType === 'GLOBAL') {
            return forkJoin({
              players: this.adminService.getPlayers(),
              sites: this.sitesService.getSites().pipe(catchError(() => of([] as SiteOption[])))
            }).pipe(map(({ players, sites }) => this.mapGlobalPlayers(players, sites)));
          }

          if (adminInfo.siteId == null) {
            throw new Error("Perimetre administrateur introuvable.");
          }

          return this.adminService
            .getPlayersBySite(adminInfo.siteId)
            .pipe(map((players) => this.mapSitePlayers(players, adminInfo)));
        }),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (players) => {
          this.players.set(players);
        },
        error: (error: HttpErrorResponse | Error) => {
          this.players.set([]);
          this.errorMessage.set(this.getErrorMessage(error));
        }
      });
  }

  private mapGlobalPlayers(players: AdminPlayerResponse[], sites: SiteOption[]): AdminPlayerListItem[] {
    const siteNames = new Map(sites.map((site) => [site.id, site.nom]));

    return players.map((player) => ({
      matricule: player.matricule,
      nom: player.nom,
      type: player.type,
      siteId: player.siteId,
      siteNom: player.siteId == null ? null : siteNames.get(player.siteId) ?? null,
      solde: player.solde
    }));
  }

  private mapSitePlayers(players: AdminSitePlayerResponse[], adminInfo: AdminInfoResponse): AdminPlayerListItem[] {
    return players.map((player) => ({
      matricule: player.matricule,
      nom: player.nom,
      type: player.type,
      siteId: adminInfo.siteId,
      siteNom: adminInfo.siteNom,
      solde: player.solde
    }));
  }

  private getErrorMessage(error: HttpErrorResponse | Error): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'Backend inaccessible.';
      }

      if (error.status === 401) {
        return 'Authentification requise.';
      }

      if (error.status === 403) {
        return 'Acces administrateur refuse.';
      }

      if (error.status === 404) {
        return 'Perimetre administrateur introuvable.';
      }

      return "Impossible de charger la liste des joueurs.";
    }

    return error.message || "Impossible de charger la liste des joueurs.";
  }
}
