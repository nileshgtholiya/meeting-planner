import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormArray, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { CreateMeeting } from '../../core/models';

@Component({
  selector: 'app-meeting-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <h2>New Meeting</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label>Title<input formControlName="title" /></label>
      <label>Description<textarea formControlName="description"></textarea></label>
      <label>Location<input formControlName="location" /></label>
      <label>Start<input type="datetime-local" formControlName="startTime" /></label>
      <label>End<input type="datetime-local" formControlName="endTime" /></label>
      <label>Timezone<input formControlName="timezone" /></label>
      <label>Recurrence
        <select formControlName="recurrence">
          <option value="NONE">None</option>
          <option value="DAILY">Daily</option>
          <option value="WEEKLY">Weekly</option>
          <option value="MONTHLY">Monthly</option>
        </select>
      </label>

      <h3>Participants</h3>
      <div formArrayName="participants">
        @for (p of participants.controls; track $index) {
          <div [formGroupName]="$index" class="participant">
            <input placeholder="Name" formControlName="name" />
            <input placeholder="Email" formControlName="email" />
            <button type="button" (click)="removeParticipant($index)">Remove</button>
          </div>
        }
      </div>
      <button type="button" (click)="addParticipant()">Add participant</button>

      <button type="submit" [disabled]="form.invalid">Create</button>
    </form>
    @if (error) { <p class="error">{{ error }}</p> }
  `,
  styles: [`
    form { display: flex; flex-direction: column; gap: 8px; max-width: 480px; }
    label { display: flex; flex-direction: column; }
    .participant { display: flex; gap: 6px; margin-bottom: 6px; }
    .error { color: #c00; }
  `]
})
export class MeetingFormComponent {
  error = '';
  form = this.fb.group({
    title: ['', Validators.required],
    description: [''],
    location: [''],
    startTime: ['', Validators.required],
    endTime: ['', Validators.required],
    timezone: [Intl.DateTimeFormat().resolvedOptions().timeZone, Validators.required],
    recurrence: ['NONE', Validators.required],
    participants: this.fb.array([])
  });

  constructor(private fb: FormBuilder, private api: ApiService, private router: Router) {}

  get participants(): FormArray {
    return this.form.get('participants') as FormArray;
  }

  addParticipant(): void {
    this.participants.push(this.fb.group({
      name: ['', Validators.required],
      email: ['']
    }));
  }

  removeParticipant(index: number): void {
    this.participants.removeAt(index);
  }

  submit(): void {
    this.error = '';
    const value = this.form.value;
    const payload: CreateMeeting = {
      title: value.title!,
      description: value.description || undefined,
      location: value.location || undefined,
      startTime: value.startTime!,
      endTime: value.endTime!,
      timezone: value.timezone!,
      recurrence: value.recurrence as CreateMeeting['recurrence'],
      participants: (value.participants as { name: string; email?: string }[]) || []
    };
    this.api.createMeeting(payload).subscribe({
      next: (m) => this.router.navigate(['/meetings', m.id]),
      error: (e) => this.error = e.error?.message || 'Could not create meeting'
    });
  }
}
