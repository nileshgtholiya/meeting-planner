import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

const TOKEN_KEY = 'mp_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));

  constructor(private http: HttpClient) {}

  get token(): string | null {
    return this.tokenSignal();
  }

  isLoggedIn(): boolean {
    return this.tokenSignal() !== null;
  }

  signup(email: string, password: string, displayName: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>('/api/auth/signup',
      { email, password, displayName }).pipe(tap(r => this.store(r.token)));
  }

  login(email: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>('/api/auth/login',
      { email, password }).pipe(tap(r => this.store(r.token)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.tokenSignal.set(null);
  }

  private store(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.tokenSignal.set(token);
  }
}
