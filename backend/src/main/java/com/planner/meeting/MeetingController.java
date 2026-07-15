package com.planner.meeting;

import com.planner.meeting.dto.CreateMeetingRequest;
import com.planner.meeting.dto.MeetingResponse;
import com.planner.meeting.dto.UpdateStatusRequest;
import com.planner.user.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping
    public List<MeetingResponse> list(@AuthenticationPrincipal User user) {
        return meetingService.listForUser(user);
    }

    @PostMapping
    public MeetingResponse create(@AuthenticationPrincipal User user,
                                  @Valid @RequestBody CreateMeetingRequest request) {
        return meetingService.create(user, request);
    }

    @GetMapping("/{id}")
    public MeetingResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return meetingService.getForUser(user, id);
    }

    @PatchMapping("/{id}/status")
    public MeetingResponse updateStatus(@AuthenticationPrincipal User user,
                                        @PathVariable Long id,
                                        @Valid @RequestBody UpdateStatusRequest request) {
        return meetingService.updateStatus(user, id, request.status());
    }
}
