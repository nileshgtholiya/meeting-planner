import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { Meeting } from '../../core/models';

@Component({
  selector: 'app-meetings-list',
  standalone: true,
  imports: [RouterLink, DatePipe],
  template: `
    <h2>My Meetings</h2>
    @if (meetings.length === 0) {
      <p>No meetings yet. <a routerLink="/meetings/new">Create one</a>.</p>
    } @else {
      <table>
        <thead>
          <tr><th>Title</th><th>Start</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
          @for (m of meetings; track m.id) {
            <tr>
              <td>{{ m.title }}</td>
              <td>{{ m.startTime | date:'short' }}</td>
              <td>{{ m.status }}</td>
              <td><a [routerLink]="['/meetings', m.id]">View</a></td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
  styles: [`
    table { border-collapse: collapse; width: 100%; }
    th, td { text-align: left; padding: 6px 10px; border-bottom: 1px solid #eee; }
  `]
})
export class MeetingsListComponent implements OnInit {
  meetings: Meeting[] = [];

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.listMeetings().subscribe(m => this.meetings = m);
  }
}
