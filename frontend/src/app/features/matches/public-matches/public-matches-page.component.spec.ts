import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { PlayerMatchSummary } from '../../../core/matches/player-match-summary.models';
import { PlayerMatchesService } from '../../../core/matches/player-matches.service';
import { PublicMatch } from '../../../core/matches/public-match.models';
import { PublicMatchesService } from '../../../core/matches/public-matches.service';
import { PublicMatchesPageComponent } from './public-matches-page.component';

describe('PublicMatchesPageComponent integration', () => {
  let fixture: ComponentFixture<PublicMatchesPageComponent>;
  let authService: { hasPlayerProfile: ReturnType<typeof vi.fn> };
  let publicMatchesService: { getPublicMatches: ReturnType<typeof vi.fn> };
  let playerMatchesService: { getMyMatches: ReturnType<typeof vi.fn> };

  const publicMatch: PublicMatch = {
    id: 42,
    dateDebut: '2099-05-22',
    heureDebut: '18:00:00',
    statut: 'PLANIFIE',
    statutTemporel: 'FUTUR',
    siteId: 1,
    siteNom: 'Padel Bruxelles',
    terrainId: 10,
    terrainNom: 'Terrain 1',
    organisateurMatricule: 'J0001',
    nbParticipants: 2,
    placesRestantes: 2,
    complet: false,
    montantParJoueur: 15
  };

  beforeEach(async () => {
    authService = { hasPlayerProfile: vi.fn() };
    publicMatchesService = { getPublicMatches: vi.fn() };
    playerMatchesService = { getMyMatches: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [PublicMatchesPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: PublicMatchesService, useValue: publicMatchesService },
        { provide: PlayerMatchesService, useValue: playerMatchesService }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders public matches returned by the service', () => {
    renderPage({ matches: [publicMatch], hasPlayerProfile: false });

    expect(publicMatchesService.getPublicMatches).toHaveBeenCalled();
    expect(pageText()).toContain('Padel Bruxelles');
    expect(pageText()).toContain('Terrain 1');
    expect(pageText()).toContain('Voir le');
  });

  it('renders the empty state when the service returns no matches', () => {
    renderPage({ matches: [], hasPlayerProfile: false });

    expect(pageText()).toContain('Aucun match public disponible');
  });

  it('renders the error state when loading public matches fails', () => {
    authService.hasPlayerProfile.mockReturnValue(false);
    publicMatchesService.getPublicMatches.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );

    fixture = TestBed.createComponent(PublicMatchesPageComponent);
    fixture.detectChanges();

    expect(pageText()).toContain('Impossible de charger les matchs publics.');
  });

  it('uses the detail-only CTA when the current player already participates', () => {
    const playerMatch = {
      id: 42,
      roleJoueur: 'PARTICIPANT'
    } as PlayerMatchSummary;

    renderPage({ matches: [publicMatch], hasPlayerProfile: true, playerMatches: [playerMatch] });

    const detailLink = fixture.nativeElement.querySelector('a[href="/matchs/42"]') as HTMLAnchorElement | null;

    expect(pageText()).toContain('Padel Bruxelles');
    expect(detailLink?.textContent?.trim()).toBe('Voir le détail');
  });

  function renderPage(options: {
    matches: PublicMatch[];
    hasPlayerProfile: boolean;
    playerMatches?: PlayerMatchSummary[];
  }): void {
    authService.hasPlayerProfile.mockReturnValue(options.hasPlayerProfile);
    publicMatchesService.getPublicMatches.mockReturnValue(of(options.matches));
    playerMatchesService.getMyMatches.mockReturnValue(of(options.playerMatches ?? []));

    fixture = TestBed.createComponent(PublicMatchesPageComponent);
    fixture.detectChanges();
  }

  function pageText(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }
});
