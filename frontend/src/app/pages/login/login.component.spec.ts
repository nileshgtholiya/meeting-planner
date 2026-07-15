import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [LoginComponent, HttpClientTestingModule, RouterTestingModule]
    });
  });

  it('creates and defaults to login mode', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component.mode).toBe('login');
  });

  it('toggles to signup mode', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance.toggle();
    expect(fixture.componentInstance.mode).toBe('signup');
  });
});
