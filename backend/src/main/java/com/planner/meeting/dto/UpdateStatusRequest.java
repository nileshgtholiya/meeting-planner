package com.planner.meeting.dto;

import com.planner.meeting.MeetingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull MeetingStatus status) {}
