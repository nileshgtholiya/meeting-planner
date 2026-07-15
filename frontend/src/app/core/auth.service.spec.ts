import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('starts logged out', () => {
    expect(service.isLoggedIn()).toBe(false);
  });

  it('stores token on login', () => {
    service.login('a@b.com', 'pw').subscribe();
    httpMock.expectOne('/api/auth/login').flush({ token: 'abc' });
    expect(service.token).toBe('abc');
    expect(service.isLoggedIn()).toBe(true);
  });

  it('clears token on logout', () => {
    service.login('a@b.com', 'pw').subscribe();
    httpMock.expectOne('/api/auth/login').flush({ token: 'abc' });
    service.logout();
    expect(service.isLoggedIn()).toBe(false);
    expect(localStorage.getItem('mp_token')).toBeNull();
  });
});
