import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <h2>{{ mode === 'login' ? 'Log In' : 'Sign Up' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      @if (mode === 'signup') {
        <label>Display name<input formControlName="displayName" /></label>
      }
      <label>Email<input type="email" formControlName="email" /></label>
      <label>Password<input type="password" formControlName="password" /></label>
      <button type="submit" [disabled]="form.invalid">
        {{ mode === 'login' ? 'Log In' : 'Sign Up' }}
      </button>
    </form>
    @if (error) { <p class="error">{{ error }}</p> }
    <button type="button" (click)="toggle()">
      {{ mode === 'login' ? 'Need an account? Sign up' : 'Have an account? Log in' }}
    </button>
  `,
  styles: [`
    form { display: flex; flex-direction: column; gap: 8px; max-width: 320px; }
    label { display: flex; flex-direction: column; }
    .error { color: #c00; }
  `]
})
export class LoginComponent {
  mode: 'login' | 'signup' = 'login';
  error = '';
  form = this.fb.group({
    displayName: [''],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  toggle(): void {
    this.mode = this.mode === 'login' ? 'signup' : 'login';
    this.error = '';
  }

  submit(): void {
    this.error = '';
    const { email, password, displayName } = this.form.value;
    const request = this.mode === 'login'
      ? this.auth.login(email!, password!)
      : this.auth.signup(email!, password!, displayName || 'User');
    request.subscribe({
      next: () => this.router.navigate(['/meetings']),
      error: (e) => this.error = e.error?.message || 'Request failed'
    });
  }
}
