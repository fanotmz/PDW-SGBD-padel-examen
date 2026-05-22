import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { jwtInterceptor } from './jwt.interceptor';

describe('jwtInterceptor', () => {
  const authServiceMock = {
    getToken: vi.fn()
  };

  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    authServiceMock.getToken.mockReset();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceMock }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('adds the Authorization header when a token is available', () => {
    authServiceMock.getToken.mockReturnValue('jwt-token');

    httpClient.get('/api/protected').subscribe();

    const request = httpTesting.expectOne('/api/protected');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush({});
  });

  it('does not add the Authorization header when no token is available', () => {
    authServiceMock.getToken.mockReturnValue(null);

    httpClient.get('/api/public').subscribe();

    const request = httpTesting.expectOne('/api/public');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
});
