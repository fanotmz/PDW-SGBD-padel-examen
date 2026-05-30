import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { MeProfile, MeStats } from '../../../core/me/me.models';
import { MeService } from '../../../core/me/me.service';
import { PaiementService } from '../../../core/payments/paiement.service';
import { RegularisationsResponse } from '../../../core/regularisations/regularisation.models';
import { RegularisationService } from '../../../core/regularisations/regularisation.service';
import { EspacePrivePageComponent } from './espace-prive-page.component';

describe('EspacePrivePageComponent integration', () => {
  let fixture: ComponentFixture<EspacePrivePageComponent>;
  let authService: {
    isAdmin: ReturnType<typeof vi.fn>;
    hasPlayerProfile: ReturnType<typeof vi.fn>;
  };
  let meService: {
    getMe: ReturnType<typeof vi.fn>;
    getMyStats: ReturnType<typeof vi.fn>;
  };
  let regularisationService: {
    getMyRegularisations: ReturnType<typeof vi.fn>;
    payLateCancellationRegularisation: ReturnType<typeof vi.fn>;
  };
  let paiementService: {
    payerParticipation: ReturnType<typeof vi.fn>;
  };

  const profile: MeProfile = {
    matricule: 'joueur.site.dev',
    nom: 'Joueur Site',
    type: 'SITE',
    siteId: 1,
    solde: 15,
    penaliteJusqua: null
  };
  const regularisations: RegularisationsResponse = {
    totalTracable: 0,
    items: []
  };
  const stats: MeStats = {
    prochainMatch: {
      id: 12,
      dateDebut: '2030-01-10T10:00:00',
      siteNom: 'Site Nord',
      terrainNom: 'Terrain 1',
      roleJoueur: 'ORGANISATEUR'
    },
    matchsCommeOrganisateur: {
      joues: 2,
      aVenir: 1,
      annules: 1
    },
    matchsCommeParticipant: {
      joues: 6,
      aVenir: 3,
      annules: 0
    },
    paiements: {
      participationsPayees: 7,
      participationsAPayer: 2,
      montantNetPaye: 105,
      montantRembourse: 15
    }
  };

  beforeEach(async () => {
    authService = {
      isAdmin: vi.fn(),
      hasPlayerProfile: vi.fn()
    };
    meService = {
      getMe: vi.fn(),
      getMyStats: vi.fn()
    };
    regularisationService = {
      getMyRegularisations: vi.fn(),
      payLateCancellationRegularisation: vi.fn()
    };
    paiementService = {
      payerParticipation: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [EspacePrivePageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: MeService, useValue: meService },
        { provide: RegularisationService, useValue: regularisationService },
        { provide: PaiementService, useValue: paiementService }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('loads and renders the current player statistics', () => {
    arrangeSuccessfulPage();

    createPage();

    expect(meService.getMyStats).toHaveBeenCalled();
    expect(pageText()).toContain('Mes statistiques');
    expect(pageText()).toContain('Prochain match');
    expect(pageText()).toContain('Site Nord');
    expect(pageText()).toContain('Terrain 1');
    expect(pageText()).toContain('Organisateur');
    expect(pageText()).toContain('Matchs comme organisateur');
    expect(pageText()).toContain('Matchs comme participant');
    expect(pageText()).toContain('Paiements');
    expect(pageText()).toContain('Participations payées');
    expect(pageText()).toContain('Participations non payées');
    expect(pageText()).toContain('Montant net payé');
    expect(pageText()).toContain('105,00');
    expect(pageText()).toContain('Montant remboursé');
    expect(pageText()).toContain('15,00');
  });

  it('renders a local message when there is no next match', () => {
    arrangeSuccessfulPage({
      ...stats,
      prochainMatch: null
    });

    createPage();

    expect(pageText()).toContain('Aucun match à venir pour le moment.');
  });

  it('keeps the private space visible when statistics loading fails', () => {
    authService.isAdmin.mockReturnValue(false);
    authService.hasPlayerProfile.mockReturnValue(true);
    meService.getMe.mockReturnValue(of(profile));
    meService.getMyStats.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );
    regularisationService.getMyRegularisations.mockReturnValue(of(regularisations));

    createPage();

    expect(pageText()).toContain('Informations personnelles');
    expect(pageText()).toContain('Solde / dette');
    expect(pageText()).toContain('Matchs à payer');
    expect(pageText()).toContain('Accès rapides');
    expect(pageText()).toContain('Les statistiques ne sont pas disponibles actuellement.');
  });

  function arrangeSuccessfulPage(statsResponse: MeStats = stats): void {
    authService.isAdmin.mockReturnValue(false);
    authService.hasPlayerProfile.mockReturnValue(true);
    meService.getMe.mockReturnValue(of(profile));
    meService.getMyStats.mockReturnValue(of(statsResponse));
    regularisationService.getMyRegularisations.mockReturnValue(of(regularisations));
  }

  function createPage(): void {
    fixture = TestBed.createComponent(EspacePrivePageComponent);
    fixture.detectChanges();
    fixture.detectChanges();
  }

  function pageText(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }
});
