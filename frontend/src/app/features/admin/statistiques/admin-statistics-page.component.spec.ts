import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import {
  AdminCaStatsResponse,
  AdminDettesStatsResponse,
  AdminInfoResponse,
  AdminMatchsStatsResponse
} from '../../../core/admin/admin.models';
import { AdminService } from '../../../core/admin/admin.service';
import { SiteOption } from '../../../core/sites/site.models';
import { SitesService } from '../../../core/sites/sites.service';
import { AdminStatisticsPageComponent } from './admin-statistics-page.component';

describe('AdminStatisticsPageComponent integration', () => {
  let fixture: ComponentFixture<AdminStatisticsPageComponent>;
  let adminService: {
    getInfo: ReturnType<typeof vi.fn>;
    getGlobalCaStats: ReturnType<typeof vi.fn>;
    getGlobalMatchsStats: ReturnType<typeof vi.fn>;
    getGlobalDettesStats: ReturnType<typeof vi.fn>;
    getSiteCaStats: ReturnType<typeof vi.fn>;
    getSiteMatchsStats: ReturnType<typeof vi.fn>;
    getSiteDettesStats: ReturnType<typeof vi.fn>;
  };
  let sitesService: { getSites: ReturnType<typeof vi.fn> };

  const globalInfo: AdminInfoResponse = {
    status: 'ok',
    adminType: 'GLOBAL',
    siteId: null,
    siteNom: null
  };
  const siteInfo: AdminInfoResponse = {
    status: 'ok',
    adminType: 'SITE',
    siteId: 2,
    siteNom: 'Padel Liège'
  };
  const sites: SiteOption[] = [
    { id: 2, nom: 'Padel Liège', ville: 'Liège', joursFermeture: [] }
  ];
  const caStats: AdminCaStatsResponse = { caTotal: 1200, from: '2099-05-01', to: '2099-05-31' };
  const matchsStats: AdminMatchsStatsResponse = { nbMatchs: 8, from: '2099-05-01', to: '2099-05-31' };
  const dettesStats: AdminDettesStatsResponse = { detteTotale: 45, nbJoueursEnDette: 2 };

  beforeEach(async () => {
    adminService = {
      getInfo: vi.fn(),
      getGlobalCaStats: vi.fn(),
      getGlobalMatchsStats: vi.fn(),
      getGlobalDettesStats: vi.fn(),
      getSiteCaStats: vi.fn(),
      getSiteMatchsStats: vi.fn(),
      getSiteDettesStats: vi.fn()
    };
    sitesService = { getSites: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [AdminStatisticsPageComponent],
      providers: [
        provideRouter([]),
        { provide: AdminService, useValue: adminService },
        { provide: SitesService, useValue: sitesService }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('loads and renders global statistics for a global admin', () => {
    arrangeGlobalAdmin();

    createPage();

    expect(sitesService.getSites).toHaveBeenCalled();
    expect(adminService.getGlobalCaStats).toHaveBeenCalled();
    expect(adminService.getGlobalMatchsStats).toHaveBeenCalled();
    expect(adminService.getGlobalDettesStats).toHaveBeenCalled();
    expect(pageText()).toContain('Tous les sites');
    expect(pageText()).toContain('1');
    expect(pageText()).toContain('200');
    expect(pageText()).toContain('8');
  });

  it('loads and renders site statistics for a site admin', () => {
    adminService.getInfo.mockReturnValue(of(siteInfo));
    arrangeSiteStats();

    createPage();

    expect(sitesService.getSites).not.toHaveBeenCalled();
    expect(adminService.getSiteCaStats).toHaveBeenCalledWith(2, expect.any(String), expect.any(String));
    expect(adminService.getSiteMatchsStats).toHaveBeenCalledWith(2, expect.any(String), expect.any(String));
    expect(adminService.getSiteDettesStats).toHaveBeenCalledWith(2);
    expect(pageText()).toContain('Padel Liège');
    expect(pageText()).toContain('Joueurs en dette');
  });

  it('shows a validation error and does not reload stats when the period is invalid', () => {
    arrangeGlobalAdmin();
    createPage();
    vi.clearAllMocks();

    const form = getComponentForm();
    form.controls.from.setValue('2099-06-01');
    form.controls.to.setValue('2099-05-01');
    submitForm();

    expect(pageText()).toContain('La date de');
    expect(adminService.getGlobalCaStats).not.toHaveBeenCalled();
    expect(adminService.getSiteCaStats).not.toHaveBeenCalled();
  });

  it('renders an error state when statistics loading fails', () => {
    adminService.getInfo.mockReturnValue(of(globalInfo));
    sitesService.getSites.mockReturnValue(of(sites));
    adminService.getGlobalCaStats.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );
    adminService.getGlobalMatchsStats.mockReturnValue(of(matchsStats));
    adminService.getGlobalDettesStats.mockReturnValue(of(dettesStats));

    createPage();

    expect(pageText()).toContain('Impossible de charger les statistiques.');
  });

  function arrangeGlobalAdmin(): void {
    adminService.getInfo.mockReturnValue(of(globalInfo));
    sitesService.getSites.mockReturnValue(of(sites));
    adminService.getGlobalCaStats.mockReturnValue(of(caStats));
    adminService.getGlobalMatchsStats.mockReturnValue(of(matchsStats));
    adminService.getGlobalDettesStats.mockReturnValue(of(dettesStats));
    arrangeSiteStats();
  }

  function arrangeSiteStats(): void {
    adminService.getSiteCaStats.mockReturnValue(of(caStats));
    adminService.getSiteMatchsStats.mockReturnValue(of(matchsStats));
    adminService.getSiteDettesStats.mockReturnValue(of(dettesStats));
  }

  function createPage(): void {
    fixture = TestBed.createComponent(AdminStatisticsPageComponent);
    fixture.detectChanges();
  }

  function getComponentForm() {
    return (fixture.componentInstance as unknown as {
      periodForm: AdminStatisticsPageComponent['periodForm'];
    }).periodForm;
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
