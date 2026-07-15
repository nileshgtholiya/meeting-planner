import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <nav>
      @if (auth.isLoggedIn()) {
        <a routerLink="/meetings">Meetings</a>
        <a routerLink="/meetings/new">New</a>
        <a routerLink="/profile">Profile</a>
        <button (click)="logout()">Logout</button>
      }
    </nav>
    <main><router-outlet /></main>
  `,
  styles: [`
    nav { display: flex; gap: 12px; padding: 12px; border-bottom: 1px solid #ddd; }
    main { padding: 16px; max-width: 800px; }
  `]
})
export class AppComponent {
  constructor(public auth: AuthService, private router: Router) {}

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
