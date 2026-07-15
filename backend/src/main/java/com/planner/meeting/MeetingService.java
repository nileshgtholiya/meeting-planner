package com.planner.meeting;

import com.planner.meeting.dto.CreateMeetingRequest;
import com.planner.meeting.dto.MeetingResponse;
import com.planner.meeting.dto.ParticipantDto;
import com.planner.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class MeetingService {

    private final MeetingRepository meetingRepository;

    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public MeetingResponse create(User owner, CreateMeetingRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        Meeting meeting = new Meeting();
        meeting.setTitle(request.title());
        meeting.setDescription(request.description());
        meeting.setLocation(request.location());
        meeting.setStartTime(request.startTime());
        meeting.setEndTime(request.endTime());
        meeting.setTimezone(request.timezone());
        meeting.setRecurrence(request.recurrence() == null
            ? Recurrence.NONE : request.recurrence());
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setOwner(owner);
        if (request.participants() != null) {
            for (ParticipantDto p : request.participants()) {
                Participant participant = new Participant();
                participant.setName(p.name());
                participant.setEmail(p.email());
                meeting.getParticipants().add(participant);
            }
        }
        return MeetingResponse.from(meetingRepository.save(meeting));
    }

    public List<MeetingResponse> listForUser(User user) {
        return meetingRepository.findByOwnerIdOrderByStartTimeAsc(user.getId())
            .stream().map(MeetingResponse::from).toList();
    }

    public MeetingResponse getForUser(User user, Long id) {
        return MeetingResponse.from(loadOwned(user, id));
    }

    public MeetingResponse updateStatus(User user, Long id, MeetingStatus status) {
        Meeting meeting = loadOwned(user, id);
        meeting.setStatus(status);
        return MeetingResponse.from(meetingRepository.save(meeting));
    }

    private Meeting loadOwned(User user, Long id) {
        Meeting meeting = meetingRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Meeting not found"));
        if (!meeting.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("Not allowed");
        }
        return meeting;
    }
}
