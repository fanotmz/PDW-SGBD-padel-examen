import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { PublicMatchesService } from './public-matches.service';

describe('PublicMatchesService', () => {
  let service: PublicMatchesService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PublicMatchesService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(PublicMatchesService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('loads public matches from the configured endpoint without filters', () => {
    service.getPublicMatches().subscribe((matches) => {
      expect(matches).toEqual([]);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/matchs/public`);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush([]);
  });

  it('sends date and site filters as query parameters', () => {
    service.getPublicMatches({ from: '2026-05-01', to: '2026-05-31', siteId: 3 }).subscribe();

    const request = httpTesting.expectOne(
      (req) => req.url === `${environment.apiBaseUrl}/matchs/public`
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('from')).toBe('2026-05-01');
    expect(request.request.params.get('to')).toBe('2026-05-31');
    expect(request.request.params.get('siteId')).toBe('3');
    request.flush([]);
  });
});
