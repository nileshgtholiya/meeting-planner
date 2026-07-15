export type Recurrence = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY';
export type MeetingStatus = 'SCHEDULED' | 'CANCELLED' | 'COMPLETED';

export interface Participant {
  name: string;
  email?: string;
}

export interface User {
  id: number;
  email: string;
  displayName: string;
  avatarUrl?: string;
}

export interface Meeting {
  id: number;
  title: string;
  description?: string;
  location?: string;
  startTime: string;
  endTime: string;
  timezone: string;
  recurrence: Recurrence;
  status: MeetingStatus;
  ownerDisplayName: string;
  participants: Participant[];
  durationMinutes: number;
  participantCount: number;
}

export interface CreateMeeting {
  title: string;
  description?: string;
  location?: string;
  startTime: string;
  endTime: string;
  timezone: string;
  recurrence: Recurrence;
  participants: Participant[];
}
