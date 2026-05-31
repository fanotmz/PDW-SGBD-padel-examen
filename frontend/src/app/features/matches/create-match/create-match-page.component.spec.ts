import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CreatedMatch, MatchSlotsResponse } from '../../../core/matches/create-match.models';
import { MatchCreationService } from '../../../core/matches/match-creation.service';
import { MeProfile } from '../../../core/me/me.models';
import { MeService } from '../../../core/me/me.service';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';
import { TerrainOption } from '../../../core/terrains/terrain.models';
import { TerrainsService } from '../../../core/terrains/terrains.service';
import { UiFeedbackService } from '../../../shared/ui/ui-feedback.service';
import { CreateMatchPageComponent } from './create-match-page.component';

describe('CreateMatchPageComponent integration', () => {
  let fixture: ComponentFixture<CreateMatchPageComponent>;
  let matchCreationService: {
    getCreneaux: ReturnType<typeof vi.fn>;
    createMatch: ReturnType<typeof vi.fn>;
  };
  let meService: { getMe: ReturnType<typeof vi.fn> };
  let sitesService: { getSites: ReturnType<typeof vi.fn> };
  let terrainsService: { getTerrains: ReturnType<typeof vi.fn> };
  let feedbackService: { showSuccess: ReturnType<typeof vi.fn> };

  const globalProfile: MeProfile = {
    matricule: 'J0002',
    nom: 'Joueur Global',
    type: 'GLOBAL',
    siteId: null,
    solde: 0
  };
  const siteProfile: MeProfile = {
    matricule: 'J0003',
    nom: 'Joueur Site',
    type: 'SITE',
    siteId: 1,
    solde: 0
  };
  const sites: SiteOption[] = [
    { id: 1, nom: 'Padel Bruxelles', ville: 'Bruxelles', joursFermeture: [] }
  ];
  const terrains: TerrainOption[] = [
    { id: 10, nom: 'Terrain 1', siteId: 1 }
  ];
  const slotsResponse: MatchSlotsResponse = {
    creneaux: ['18:00', '19:45'],
    message: null,
    annee: 2099,
    heureOuverture: '08:00',
    heureFermeture: '22:00',
    dureeMatchMinutes: 90,
    bufferMinutes: 15
  };
  const createdMatch: CreatedMatch = {
    id: 99,
    terrainId: 10,
    terrainNom: 'Terrain 1',
    siteId: 1,
    organisateurMatricule: 'J0002',
    dateDebut: '2099-05-22T18:00:00',
    visibilite: 'PUBLIC',
    statut: 'PLANIFIE',
    nbParticipants: 1,
    montantTotal: 60,
    montantPaye: 15,
    resteAPayer: 45
  };

  beforeEach(async () => {
    matchCreationService = {
      getCreneaux: vi.fn(),
      createMatch: vi.fn()
    };
    meService = { getMe: vi.fn() };
    sitesService = { getSites: vi.fn() };
    terrainsService = { getTerrains: vi.fn() };
    feedbackService = { showSuccess: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CreateMatchPageComponent],
      providers: [
        provideRouter([]),
        { provide: MatchCreationService, useValue: matchCreationService },
        { provide: MeService, useValue: meService },
        { provide: SitesService, useValue: sitesService },
        { provide: TerrainsService, useValue: terrainsService },
        { provide: UiFeedbackService, useValue: feedbackService }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('loads profile and sites before rendering the creation form', () => {
    arrangeInitialData(globalProfile);

    createPage();

    expect(meService.getMe).toHaveBeenCalled();
    expect(sitesService.getSites).toHaveBeenCalled();
    expect(pageText()).toContain('Créer un match');
    expect(pageText()).toContain('Choisissez un site');
    expect(pageText()).toContain('Créer le match');
  });

  it('shows the locked site information for a SITE player profile', () => {
    arrangeInitialData(siteProfile);

    createPage();

    const lockedSiteInput = fixture.nativeElement.querySelector('input[readonly]') as HTMLInputElement | null;

    expect(lockedSiteInput?.value).toBe('Padel Bruxelles - Bruxelles');
    expect(pageText()).toContain('Votre abonnement SITE permet de');
  });

  it('loads and displays available slots after site, terrain and date are selected', () => {
    arrangeInitialData(globalProfile);
    createPage();

    const form = getComponentForm();
    form.controls.siteId.setValue(1);
    fixture.detectChanges();
    form.controls.terrainId.setValue(10);
    form.controls.date.setValue(new Date(2099, 4, 22));
    fixture.detectChanges();

    expect(terrainsService.getTerrains).toHaveBeenCalledWith(1);
    expect(matchCreationService.getCreneaux).toHaveBeenCalledWith(10, '2099-05-22');
    expect(pageText()).toContain('Créneaux disponibles');
    expect(pageText()).toContain('19:45');
  });

  it('creates a match with the payload produced by the form and renders the success summary', () => {
    arrangeInitialData(globalProfile);
    matchCreationService.createMatch.mockReturnValue(of(createdMatch));
    createPage();

    const form = getComponentForm();
    form.controls.siteId.setValue(1);
    form.controls.terrainId.setValue(10);
    form.controls.date.setValue(new Date(2099, 4, 22));
    form.controls.heure.setValue('18:00');
    form.controls.heure.enable();
    form.controls.visibilite.setValue('PUBLIC');
    fixture.detectChanges();
    submitForm();

    expect(matchCreationService.createMatch).toHaveBeenCalledWith({
      terrainId: 10,
      dateDebut: '2099-05-22T18:00:00',
      visibilite: 'PUBLIC'
    });
    expect(feedbackService.showSuccess).toHaveBeenCalled();
    expect(pageText()).toContain('Terrain 1');
    expect(pageText()).toContain('Voir mes matchs');
  });

  function arrangeInitialData(profile: MeProfile): void {
    meService.getMe.mockReturnValue(of(profile));
    sitesService.getSites.mockReturnValue(of(sites));
    terrainsService.getTerrains.mockReturnValue(of(terrains));
    matchCreationService.getCreneaux.mockReturnValue(of(slotsResponse));
    matchCreationService.createMatch.mockReturnValue(of(createdMatch));
  }

  function createPage(): void {
    fixture = TestBed.createComponent(CreateMatchPageComponent);
    fixture.detectChanges();
  }

  function getComponentForm() {
    return (fixture.componentInstance as unknown as { form: CreateMatchPageComponent['form'] }).form;
  }

  function submitForm(): void {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }

  function pageText(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }
});
