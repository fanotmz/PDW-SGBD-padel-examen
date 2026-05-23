import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AdminSiteMatchSummaryResponse } from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';
import { AdminSiteMatchesPageComponent } from './admin-site-matches-page.component';

describe('AdminSiteMatchesPageComponent integration', () => {
  let fixture: ComponentFixture<AdminSiteMatchesPageComponent>;
  let adminService: { getSiteMatches: ReturnType<typeof vi.fn> };
  let router: { navigateByUrl: ReturnType<typeof vi.fn> };
  let routeSiteId = '7';

  const siteMatch: AdminSiteMatchSummaryResponse = {
    id: 31,
    dateDebut: '2099-07-15',
    heureDebut: '20:00:00',
    siteId: 7,
    siteNom: 'Padel Namur',
    terrainId: 30,
    terrainNom: 'Terrain Admin',
    organisateurMatricule: 'J0099',
    organisateurNom: 'Admin Joueur',
    visibilite: 'PUBLIC',
    statut: 'PLANIFIE',
    nbParticipants: 3,
    placesRestantes: 1,
    peutAnnuler: true,
    passe: false
  };

  beforeEach(async () => {
    adminService = { getSiteMatches: vi.fn() };
    router = { navigateByUrl: vi.fn() };
    routeSiteId = '7';

    await TestBed.configureTestingModule({
      imports: [AdminSiteMatchesPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useFactory: () => ({
            snapshot: {
              paramMap: convertToParamMap({ siteId: routeSiteId })
            }
          })
        },
        { provide: AdminService, useValue: adminService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('loads matches for the site id from the route and renders the list', () => {
    adminService.getSiteMatches.mockReturnValue(of([siteMatch]));

    createPage();

    expect(adminService.getSiteMatches).toHaveBeenCalledWith(7, {
      scope: 'ALL',
      from: '',
      to: '',
      statut: '',
      visibilite: ''
    });
    expect(pageText()).toContain('Padel Namur');
    expect(pageText()).toContain('Terrain Admin');
    expect(pageText()).toContain('Admin Joueur');
    expect(pageText()).toContain('Voir détail');
  });

  it('renders the empty state when no site match matches the filters', () => {
    adminService.getSiteMatches.mockReturnValue(of([]));

    createPage();

    expect(pageText()).toContain('Aucun match ne correspond aux filtres sélectionnés.');
  });

  it('renders an error and does not call the service when the route site id is invalid', () => {
    routeSiteId = 'invalid';
    adminService.getSiteMatches.mockReturnValue(of([siteMatch]));

    createPage();

    expect(adminService.getSiteMatches).not.toHaveBeenCalled();
    expect(pageText()).toContain('Identifiant de site invalide.');
  });

  it('reloads matches with the selected filters', () => {
    adminService.getSiteMatches.mockReturnValue(of([siteMatch]));
    createPage();
    adminService.getSiteMatches.mockClear();

    const dateInputs = fixture.nativeElement.querySelectorAll('input[type="date"]') as NodeListOf<HTMLInputElement>;
    dateInputs[0].value = '2099-07-01';
    dateInputs[0].dispatchEvent(new Event('change', { bubbles: true }));
    dateInputs[1].value = '2099-07-31';
    dateInputs[1].dispatchEvent(new Event('change', { bubbles: true }));

    const selects = fixture.nativeElement.querySelectorAll('select') as NodeListOf<HTMLSelectElement>;
    selects[0].value = 'UPCOMING';
    selects[0].dispatchEvent(new Event('change', { bubbles: true }));
    selects[1].value = 'PUBLIC';
    selects[1].dispatchEvent(new Event('change', { bubbles: true }));

    const applyButton = fixture.nativeElement.querySelector('.filter-button.is-primary') as HTMLButtonElement;
    applyButton.click();
    fixture.detectChanges();

    expect(adminService.getSiteMatches).toHaveBeenCalledWith(7, {
      scope: 'UPCOMING_PLANNED',
      from: '2099-07-01',
      to: '2099-07-31',
      statut: '',
      visibilite: 'PUBLIC'
    });
  });

  function createPage(): void {
    fixture = TestBed.createComponent(AdminSiteMatchesPageComponent);
    fixture.detectChanges();
  }

  function pageText(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }
});
