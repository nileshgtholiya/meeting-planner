import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { User } from '../../core/models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [],
  template: `
    @if (user) {
      <h2>{{ user.displayName }}</h2>
      <p>{{ user.email }}</p>
      @if (user.avatarUrl) {
        <img [src]="user.avatarUrl" alt="avatar" width="120" height="120" />
      }
      <div>
        <input type="file" accept="image/png,image/jpeg" (change)="onFile($event)" />
        <button (click)="upload()" [disabled]="!file">Upload avatar</button>
      </div>
      @if (error) { <p class="error">{{ error }}</p> }
    }
  `,
  styles: [`.error { color: #c00; } img { border-radius: 50%; object-fit: cover; }`]
})
export class ProfileComponent implements OnInit {
  user?: User;
  file?: File;
  error = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.me().subscribe(u => this.user = u);
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = input.files?.[0] || undefined;
  }

  upload(): void {
    if (!this.file) return;
    this.error = '';
    this.api.uploadAvatar(this.file).subscribe({
      next: (u) => { this.user = u; this.file = undefined; },
      error: (e) => this.error = e.error?.message || 'Upload failed'
    });
  }
}
