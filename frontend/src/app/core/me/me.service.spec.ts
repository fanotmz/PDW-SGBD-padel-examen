import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { MeProfile, MeStats } from './me.models';
import { MeService } from './me.service';

describe('MeService', () => {
  let service: MeService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MeService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(MeService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('loads the current player profile', () => {
    const response: MeProfile = {
      matricule: 'joueur.site.dev',
      nom: 'Joueur Site',
      type: 'SITE',
      siteId: 1,
      solde: 0,
      penaliteJusqua: null
    };

    service.getMe().subscribe((profile) => {
      expect(profile).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/me`);
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });

  it('loads the current player statistics', () => {
    const response: MeStats = {
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

    service.getMyStats().subscribe((stats) => {
      expect(stats).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/me/stats`);
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });
});
