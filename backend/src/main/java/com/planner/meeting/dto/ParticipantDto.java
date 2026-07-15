package com.planner.meeting.dto;

import jakarta.validation.constraints.NotBlank;

public record ParticipantDto(@NotBlank String name, String email) {}
