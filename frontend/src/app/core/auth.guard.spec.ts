import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { runInInjectionContext, EnvironmentInjector } from '@angular/core';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let injector: EnvironmentInjector;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    injector = TestBed.inject(EnvironmentInjector);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('blocks and redirects when logged out', () => {
    const result = runInInjectionContext(injector,
      () => authGuard({} as any, {} as any));
    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('allows when logged in', () => {
    TestBed.inject(AuthService)['store']('token');
    const result = runInInjectionContext(injector,
      () => authGuard({} as any, {} as any));
    expect(result).toBe(true);
  });
});
