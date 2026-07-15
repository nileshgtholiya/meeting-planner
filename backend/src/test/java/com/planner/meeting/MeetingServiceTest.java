package com.planner.meeting;

import com.planner.meeting.dto.CreateMeetingRequest;
import com.planner.meeting.dto.MeetingResponse;
import com.planner.meeting.dto.ParticipantDto;
import com.planner.user.User;
import com.planner.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MeetingServiceTest {

    private MeetingRepository meetingRepo;
    private UserRepository userRepo;
    private MeetingService service;
    private User alice;
    private User bob;

    @BeforeEach
    void setup() {
        meetingRepo = mock(MeetingRepository.class);
        userRepo = mock(UserRepository.class);
        Map<Long, Meeting> store = new HashMap<>();
        AtomicLong seq = new AtomicLong(1);
        when(meetingRepo.save(any(Meeting.class))).thenAnswer(i -> {
            Meeting m = i.getArgument(0);
            if (m.getId() == null) {
                try {
                    java.lang.reflect.Field f = Meeting.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(m, seq.getAndIncrement());
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            store.put(m.getId(), m);
            return m;
        });
        when(meetingRepo.findById(anyLong()))
            .thenAnswer(i -> Optional.ofNullable(store.get(i.getArgument(0))));

        alice = user(1L, "alice@b.com", "Alice");
        bob = user(2L, "bob@b.com", "Bob");
        when(userRepo.findByEmail("alice@b.com")).thenReturn(Optional.of(alice));
        when(userRepo.findByEmail("bob@b.com")).thenReturn(Optional.of(bob));

        service = new MeetingService(meetingRepo);
    }

    private User user(Long id, String email, String name) {
        User u = new User();
        try {
            java.lang.reflect.Field f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        u.setEmail(email);
        u.setDisplayName(name);
        return u;
    }

    private CreateMeetingRequest req(LocalDateTime start, LocalDateTime end) {
        return new CreateMeetingRequest("Standup", "Daily", "Room 1",
            start, end, "Europe/London", Recurrence.DAILY,
            List.of(new ParticipantDto("Carol", "carol@b.com")));
    }

    @Test
    void createComputesDurationAndParticipantCount() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 9, 0);
        MeetingResponse res = service.create(alice, req(start, start.plusMinutes(30)));
        assertEquals(30, res.durationMinutes());
        assertEquals(1, res.participantCount());
        assertEquals("Alice", res.ownerDisplayName());
    }

    @Test
    void endBeforeStartRejected() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 9, 0);
        assertThrows(IllegalArgumentException.class,
            () -> service.create(alice, req(start, start.minusMinutes(1))));
    }

    @Test
    void endEqualsStartRejected() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 9, 0);
        assertThrows(IllegalArgumentException.class,
            () -> service.create(alice, req(start, start)));
    }

    @Test
    void ownerCanReadOwnMeeting() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 9, 0);
        MeetingResponse created = service.create(alice, req(start, start.plusHours(1)));
        MeetingResponse fetched = service.getForUser(alice, created.id());
        assertEquals(created.id(), fetched.id());
    }

    @Test
    void otherUserCannotReadMeeting() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 9, 0);
        MeetingResponse created = service.create(alice, req(start, start.plusHours(1)));
        assertThrows(SecurityException.class,
            () -> service.getForUser(bob, created.id()));
    }

    @Test
    void missingMeetingThrowsNotFound() {
        assertThrows(java.util.NoSuchElementException.class,
            () -> service.getForUser(alice, 999L));
    }
}
