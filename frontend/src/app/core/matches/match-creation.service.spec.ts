import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { CreateMatchPayload, CreatedMatch, MatchSlotsResponse } from './create-match.models';
import { MatchCreationService } from './match-creation.service';

describe('MatchCreationService', () => {
  let service: MatchCreationService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MatchCreationService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(MatchCreationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('posts the create match payload to the configured endpoint', () => {
    const payload = {
      terrainId: 12,
      dateDebut: '2026-05-22T18:00:00',
      visibilite: 'PUBLIC'
    } satisfies CreateMatchPayload;
    const response = {
      id: 99,
      terrainId: 12,
      terrainNom: 'Terrain 1',
      siteId: 2,
      organisateurMatricule: 'J0001',
      dateDebut: '2026-05-22T18:00:00',
      visibilite: 'PUBLIC',
      statut: 'PLANIFIE',
      nbParticipants: 1,
      montantTotal: 60,
      montantPaye: 15,
      resteAPayer: 45
    } satisfies CreatedMatch;

    service.createMatch(payload).subscribe((createdMatch) => {
      expect(createdMatch).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/matchs`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(response);
  });

  it('loads available slots with terrain and date query parameters', () => {
    const response = {
      creneaux: [],
      message: null,
      annee: 2026,
      heureOuverture: '08:00',
      heureFermeture: '22:00',
      dureeMatchMinutes: 90,
      bufferMinutes: 15
    } satisfies MatchSlotsResponse;

    service.getCreneaux(12, '2026-05-22').subscribe((slots) => {
      expect(slots).toEqual(response);
    });

    const request = httpTesting.expectOne(
      (req) => req.url === `${environment.apiBaseUrl}/matchs/creneaux`
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('terrainId')).toBe('12');
    expect(request.request.params.get('date')).toBe('2026-05-22');
    request.flush(response);
  });
});
