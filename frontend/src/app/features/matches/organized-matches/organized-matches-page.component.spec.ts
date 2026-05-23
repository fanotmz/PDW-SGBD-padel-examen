import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { OrganizedMatchSummary } from '../../../core/matches/organized-match-summary.models';
import { OrganizedMatchesService } from '../../../core/matches/organized-matches.service';
import { OrganizedMatchesPageComponent } from './organized-matches-page.component';

describe('OrganizedMatchesPageComponent integration', () => {
  let fixture: ComponentFixture<OrganizedMatchesPageComponent>;
  let organizedMatchesService: { getMyOrganizedMatches: ReturnType<typeof vi.fn> };

  const organizedMatch: OrganizedMatchSummary = {
    id: 21,
    dateDebut: '2099-06-10T19:00:00',
    siteId: 2,
    siteNom: 'Padel Liège',
    terrainId: 20,
    terrainNom: 'Terrain Central',
    visibilite: 'PRIVE',
    statut: 'PLANIFIE',
    nbParticipants: 2,
    placesRestantes: 2,
    complet: false,
    statutTemporel: 'FUTUR',
    joursAvantMatch: 6,
    risquePenaliteJ1: true
  };

  beforeEach(async () => {
    organizedMatchesService = { getMyOrganizedMatches: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [OrganizedMatchesPageComponent],
      providers: [
        provideRouter([]),
        { provide: OrganizedMatchesService, useValue: organizedMatchesService }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders organized matches returned by the service', () => {
    organizedMatchesService.getMyOrganizedMatches.mockReturnValue(of([organizedMatch]));

    createPage();

    expect(organizedMatchesService.getMyOrganizedMatches).toHaveBeenCalled();
    expect(pageText()).toContain('Padel Liège');
    expect(pageText()).toContain('Terrain Central');
    expect(pageText()).toContain('Participants');
    expect(pageText()).toContain('Voir le détail');
  });

  it('renders the empty state when no organized match exists', () => {
    organizedMatchesService.getMyOrganizedMatches.mockReturnValue(of([]));

    createPage();

    expect(pageText()).toContain("Vous n'avez aucun match organisé pour le moment.");
  });

  it('renders the error state when loading organized matches fails', () => {
    organizedMatchesService.getMyOrganizedMatches.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );

    createPage();

    expect(pageText()).toContain('Impossible de charger vos matchs organisés.');
  });

  function createPage(): void {
    fixture = TestBed.createComponent(OrganizedMatchesPageComponent);
    fixture.detectChanges();
  }

  function pageText(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }
});
