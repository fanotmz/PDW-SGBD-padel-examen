import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import {
  AdminCaStatsResponse,
  AdminInfoResponse,
  AdminSiteMatchSummaryResponse,
  AdminSiteScheduleResponse,
  RegistrationDecisionResponse
} from './admin.models';
import { AdminService } from './admin.service';

describe('AdminService', () => {
  let service: AdminService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AdminService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AdminService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('loads admin info from the configured endpoint', () => {
    const response = { status: 'OK', adminType: 'SITE', siteId: 2, siteNom: 'Padel Bruxelles' } as AdminInfoResponse;

    service.getInfo().subscribe((info) => {
      expect(info).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/admin/info`);
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });

  it('loads site matches with all supported filters', () => {
    service.getSiteMatches(2, {
      from: '2026-05-01',
      to: '2026-05-31',
      statut: 'PLANIFIE',
      visibilite: 'PUBLIC',
      scope: 'UPCOMING_PLANNED'
    }).subscribe((matches) => {
      expect(matches).toEqual([]);
    });

    const request = httpTesting.expectOne(
      (req) => req.url === `${environment.apiBaseUrl}/admin/sites/2/matchs`
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('from')).toBe('2026-05-01');
    expect(request.request.params.get('to')).toBe('2026-05-31');
    expect(request.request.params.get('statut')).toBe('PLANIFIE');
    expect(request.request.params.get('visibilite')).toBe('PUBLIC');
    expect(request.request.params.get('scope')).toBe('UPCOMING_PLANNED');
    request.flush([] as AdminSiteMatchSummaryResponse[]);
  });

  it('loads site revenue stats with date filters', () => {
    const response = { caTotal: 1200, from: '2026-05-01', to: '2026-05-31' } as AdminCaStatsResponse;

    service.getSiteCaStats(2, '2026-05-01', '2026-05-31').subscribe((stats) => {
      expect(stats).toEqual(response);
    });

    const request = httpTesting.expectOne(
      (req) => req.url === `${environment.apiBaseUrl}/admin/sites/2/stats/ca`
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('from')).toBe('2026-05-01');
    expect(request.request.params.get('to')).toBe('2026-05-31');
    request.flush(response);
  });

  it('creates a site schedule with the provided payload', () => {
    const payload = { annee: 2026, heureOuverture: '08:00', heureFermeture: '22:00' };
    const response = { id: 7, siteId: 2, ...payload } as AdminSiteScheduleResponse;

    service.createSiteSchedule(2, payload).subscribe((schedule) => {
      expect(schedule).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/admin/sites/2/horaires`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(response);
  });

  it('validates a registration request with the provided payload', () => {
    const payload = { typeAbonnementFinal: 'SITE' as const, siteIdFinal: 2 };
    const response = { userId: 5, status: 'VALIDATED' } as RegistrationDecisionResponse;

    service.validateRegistration(5, payload).subscribe((decision) => {
      expect(decision).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/admin/inscriptions/5/validate`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(response);
  });
});
