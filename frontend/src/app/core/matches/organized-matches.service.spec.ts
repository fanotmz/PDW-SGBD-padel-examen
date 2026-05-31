import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { OrganizedMatchesService } from './organized-matches.service';

describe('OrganizedMatchesService', () => {
  let service: OrganizedMatchesService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        OrganizedMatchesService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(OrganizedMatchesService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('loads matches organized by the current player', () => {
    service.getMyOrganizedMatches().subscribe((matches) => {
      expect(matches).toEqual([]);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/me/matchs/organises`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
