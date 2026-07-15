import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { Meeting, MeetingStatus } from '../../core/models';

@Component({
  selector: 'app-meeting-detail',
  standalone: true,
  imports: [DatePipe],
  template: `
    @if (meeting) {
      <h2>{{ meeting.title }}</h2>
      <p><strong>Status:</strong> {{ meeting.status }}</p>
      <p><strong>When:</strong> {{ meeting.startTime | date:'medium' }}
        &ndash; {{ meeting.endTime | date:'shortTime' }} ({{ meeting.timezone }})</p>
      <p><strong>Duration:</strong> {{ meeting.durationMinutes }} minutes</p>
      <p><strong>Recurrence:</strong> {{ meeting.recurrence }}</p>
      <p><strong>Location:</strong> {{ meeting.location || '-' }}</p>
      <p><strong>Owner:</strong> {{ meeting.ownerDisplayName }}</p>
      @if (meeting.description) { <p>{{ meeting.description }}</p> }

      <h3>Participants ({{ meeting.participantCount }})</h3>
      <ul>
        @for (p of meeting.participants; track p.email) {
          <li>{{ p.name }} @if (p.email) { <span>&lt;{{ p.email }}&gt;</span> }</li>
        }
      </ul>

      <label>Change status
        <select [value]="meeting.status" (change)="changeStatus($event)">
          <option value="SCHEDULED">Scheduled</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="COMPLETED">Completed</option>
        </select>
      </label>
    } @else if (error) {
      <p class="error">{{ error }}</p>
    }
  `,
  styles: [`.error { color: #c00; }`]
})
export class MeetingDetailComponent implements OnInit {
  meeting?: Meeting;
  error = '';

  constructor(private route: ActivatedRoute, private api: ApiService) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getMeeting(id).subscribe({
      next: (m) => this.meeting = m,
      error: () => this.error = 'Meeting not found or not accessible'
    });
  }

  changeStatus(event: Event): void {
    const status = (event.target as HTMLSelectElement).value as MeetingStatus;
    this.api.updateStatus(this.meeting!.id, status)
      .subscribe(m => this.meeting = m);
  }
}
