import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { MatchDetail } from '../../../core/matches/match-detail.models';
import { MatchDetailService } from '../../../core/matches/match-detail.service';
import { MatchParticipationService } from '../../../core/matches/match-participation.service';
import { MeProfile } from '../../../core/me/me.models';
import { MeService } from '../../../core/me/me.service';
import { MatchDetailPageComponent } from './match-detail-page.component';

describe('MatchDetailPageComponent integration', () => {
  let fixture: ComponentFixture<MatchDetailPageComponent>;
  let authService: {
    isAdmin: ReturnType<typeof vi.fn>;
    hasPlayerProfile: ReturnType<typeof vi.fn>;
  };
  let matchDetailService: {
    getMatchDetail: ReturnType<typeof vi.fn>;
    cancelMatch: ReturnType<typeof vi.fn>;
  };
  let matchParticipationService: {
    rejoindreMatchPublic: ReturnType<typeof vi.fn>;
    ajouterJoueurPrive: ReturnType<typeof vi.fn>;
  };
  let meService: { getMe: ReturnType<typeof vi.fn> };
  let router: { navigateByUrl: ReturnType<typeof vi.fn> };
  let location: { back: ReturnType<typeof vi.fn> };
  let routeId = '42';

  const baseMatch: MatchDetail = {
    id: 42,
    dateDebut: '2099-05-22',
    heureDebut: '18:00:00',
    siteId: 1,
    siteNom: 'Padel Bruxelles',
    terrainId: 10,
    terrainNom: 'Terrain 1',
    organisateurMatricule: 'J0001',
    organisateurNom: 'Organisateur',
    visibilite: 'PUBLIC',
    statut: 'PLANIFIE',
    nbParticipants: 1,
    placesRestantes: 3,
    complet: false,
    peutAjouterJoueurPrive: false,
    peutAnnuler: false,
    montantTotal: 60,
    montantPaye: 15,
    resteAPayer: 45,
    montantRembourse: 0,
    participants: [{ matricule: 'J0001', nom: 'Organisateur' }]
  };

  const currentProfile: MeProfile = {
    matricule: 'J0002',
    nom: 'Participant potentiel',
    type: 'GLOBAL',
    siteId: null,
    solde: 0
  };

  beforeEach(async () => {
    authService = {
      isAdmin: vi.fn(),
      hasPlayerProfile: vi.fn()
    };
    matchDetailService = {
      getMatchDetail: vi.fn(),
      cancelMatch: vi.fn()
    };
    matchParticipationService = {
      rejoindreMatchPublic: vi.fn(),
      ajouterJoueurPrive: vi.fn()
    };
    meService = { getMe: vi.fn() };
    router = { navigateByUrl: vi.fn() };
    location = { back: vi.fn() };
    routeId = '42';

    await TestBed.configureTestingModule({
      imports: [MatchDetailPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useFactory: () => ({
            snapshot: {
              paramMap: convertToParamMap({ id: routeId })
            }
          })
        },
        { provide: AuthService, useValue: authService },
        { provide: MatchDetailService, useValue: matchDetailService },
        { provide: MatchParticipationService, useValue: matchParticipationService },
        { provide: MeService, useValue: meService },
        { provide: Router, useValue: router },
        { provide: Location, useValue: location }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders an error and does not call the service when the route id is invalid', () => {
    routeId = 'invalid';

    createPage();

    expect(matchDetailService.getMatchDetail).not.toHaveBeenCalled();
    expect(pageText()).toContain('Identifiant de match invalide.');
  });

  it('loads the match from the route id and renders the main information', () => {
    arrangePlayerDetail(baseMatch, currentProfile);

    createPage();

    expect(matchDetailService.getMatchDetail).toHaveBeenCalledWith(42);
    expect(meService.getMe).toHaveBeenCalled();
    expect(pageText()).toContain('Padel Bruxelles');
    expect(pageText()).toContain('Terrain 1');
    expect(pageText()).toContain('Organisateur');
  });

  it('shows the join action for an open public match when the player is not already participating', () => {
    arrangePlayerDetail(baseMatch, currentProfile);

    createPage();

    expect(pageText()).toContain('Rejoindre et payer');
  });

  it('does not show the join action when the current player already participates', () => {
    arrangePlayerDetail(
      {
        ...baseMatch,
        participants: [
          ...baseMatch.participants,
          { matricule: currentProfile.matricule, nom: currentProfile.nom }
        ]
      },
      currentProfile
    );

    createPage();

    expect(pageText()).not.toContain('Rejoindre et payer');
  });

  it('renders a load error when the match cannot be found', () => {
    authService.isAdmin.mockReturnValue(false);
    authService.hasPlayerProfile.mockReturnValue(true);
    matchDetailService.getMatchDetail.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 }))
    );
    meService.getMe.mockReturnValue(of(currentProfile));

    createPage();

    expect(pageText()).toContain('Match introuvable.');
  });

  function arrangePlayerDetail(match: MatchDetail, profile: MeProfile): void {
    authService.isAdmin.mockReturnValue(false);
    authService.hasPlayerProfile.mockReturnValue(true);
    matchDetailService.getMatchDetail.mockReturnValue(of(match));
    meService.getMe.mockReturnValue(of(profile));
  }

  function createPage(): void {
    fixture = TestBed.createComponent(MatchDetailPageComponent);
    fixture.detectChanges();
  }

  function pageText(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }
});
