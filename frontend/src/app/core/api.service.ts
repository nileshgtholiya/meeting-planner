import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateMeeting, Meeting, MeetingStatus, User } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  me(): Observable<User> {
    return this.http.get<User>('/api/me');
  }

  uploadAvatar(file: File): Observable<User> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<User>('/api/me/avatar', form);
  }

  listMeetings(): Observable<Meeting[]> {
    return this.http.get<Meeting[]>('/api/meetings');
  }

  createMeeting(payload: CreateMeeting): Observable<Meeting> {
    return this.http.post<Meeting>('/api/meetings', payload);
  }

  getMeeting(id: number): Observable<Meeting> {
    return this.http.get<Meeting>(`/api/meetings/${id}`);
  }

  updateStatus(id: number, status: MeetingStatus): Observable<Meeting> {
    return this.http.patch<Meeting>(`/api/meetings/${id}/status`, { status });
  }
}
