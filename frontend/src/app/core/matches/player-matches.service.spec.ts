import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { PlayerMatchesService } from './player-matches.service';

describe('PlayerMatchesService', () => {
  let service: PlayerMatchesService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PlayerMatchesService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(PlayerMatchesService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('loads matches linked to the current player', () => {
    service.getMyMatches().subscribe((matches) => {
      expect(matches).toEqual([]);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/me/matchs`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
