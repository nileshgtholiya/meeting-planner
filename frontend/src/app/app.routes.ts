import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'meetings' },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'meetings',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/meetings-list/meetings-list.component').then(m => m.MeetingsListComponent)
  },
  {
    path: 'meetings/new',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/meeting-form/meeting-form.component').then(m => m.MeetingFormComponent)
  },
  {
    path: 'meetings/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/meeting-detail/meeting-detail.component').then(m => m.MeetingDetailComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent)
  },
  { path: '**', redirectTo: 'meetings' }
];
