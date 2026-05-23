import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PlayerMatchSummary } from '../../../core/matches/player-match-summary.models';
import { PlayerMatchesService } from '../../../core/matches/player-matches.service';
import { MyMatchesPageComponent } from './my-matches-page.component';

describe('MyMatchesPageComponent integration', () => {
  let fixture: ComponentFixture<MyMatchesPageComponent>;
  let playerMatchesService: { getMyMatches: ReturnType<typeof vi.fn> };

  const playerMatch: PlayerMatchSummary = {
    id: 11,
    dateDebut: '2099-05-22T18:00:00',
    siteId: 1,
    siteNom: 'Padel Bruxelles',
    terrainId: 10,
    terrainNom: 'Terrain 1',
    visibilite: 'PUBLIC',
    statut: 'PLANIFIE',
    roleJoueur: 'PARTICIPANT',
    statutTemporel: 'FUTUR',
    joursAvantMatch: 4,
    paiementJoueurEffectue: true,
    participationId: 100,
    montantPayeJoueur: 15,
    montantRestantJoueur: 0,
    peutPayerParticipation: false
  };

  beforeEach(async () => {
    playerMatchesService = { getMyMatches: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [MyMatchesPageComponent],
      providers: [
        provideRouter([]),
        { provide: PlayerMatchesService, useValue: playerMatchesService }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders the player matches returned by the service', () => {
    playerMatchesService.getMyMatches.mockReturnValue(of([playerMatch]));

    createPage();

    expect(playerMatchesService.getMyMatches).toHaveBeenCalled();
    expect(pageText()).toContain('Padel Bruxelles');
    expect(pageText()).toContain('Terrain 1');
    expect(pageText()).toContain('Voir le détail');
  });

  it('renders the empty state when the player has no matches', () => {
    playerMatchesService.getMyMatches.mockReturnValue(of([]));

    createPage();

    expect(pageText()).toContain("Vous n'avez aucun match pour le moment.");
  });

  it('renders the error state when loading player matches fails', () => {
    playerMatchesService.getMyMatches.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );

    createPage();

    expect(pageText()).toContain('Impossible de charger vos matchs.');
  });

  function createPage(): void {
    fixture = TestBed.createComponent(MyMatchesPageComponent);
    fixture.detectChanges();
  }

  function pageText(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }
});
