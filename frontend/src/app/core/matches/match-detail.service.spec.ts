import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { MatchDetail } from './match-detail.models';
import { MatchDetailService } from './match-detail.service';

describe('MatchDetailService', () => {
  let service: MatchDetailService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MatchDetailService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(MatchDetailService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('loads a match detail by id', () => {
    const response = { id: 42 } as MatchDetail;

    service.getMatchDetail(42).subscribe((match) => {
      expect(match).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/matchs/42`);
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });

  it('posts match cancellation with a null body', () => {
    service.cancelMatch(42).subscribe((response) => {
      expect(response).toBeNull();
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/matchs/42/annulation`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush(null);
  });
});
