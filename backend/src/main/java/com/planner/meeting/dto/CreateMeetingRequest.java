package com.planner.meeting.dto;

import com.planner.meeting.Recurrence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateMeetingRequest(
    @NotBlank String title,
    String description,
    String location,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    @NotBlank String timezone,
    Recurrence recurrence,
    @Valid List<ParticipantDto> participants) {}
