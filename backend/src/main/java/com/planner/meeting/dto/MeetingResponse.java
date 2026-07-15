package com.planner.meeting.dto;

import com.planner.meeting.Meeting;
import com.planner.meeting.MeetingStatus;
import com.planner.meeting.Recurrence;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingResponse(
    Long id,
    String title,
    String description,
    String location,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String timezone,
    Recurrence recurrence,
    MeetingStatus status,
    String ownerDisplayName,
    List<ParticipantDto> participants,
    long durationMinutes,
    int participantCount) {

    public static MeetingResponse from(Meeting m) {
        List<ParticipantDto> ps = m.getParticipants().stream()
            .map(p -> new ParticipantDto(p.getName(), p.getEmail()))
            .toList();
        long minutes = Duration.between(m.getStartTime(), m.getEndTime()).toMinutes();
        return new MeetingResponse(
            m.getId(), m.getTitle(), m.getDescription(), m.getLocation(),
            m.getStartTime(), m.getEndTime(), m.getTimezone(),
            m.getRecurrence(), m.getStatus(),
            m.getOwner().getDisplayName(), ps, minutes, ps.size());
    }
}
