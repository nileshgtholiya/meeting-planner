package com.planner.meeting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String signupAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "email", email, "password", "secret123", "displayName", "Tester"));
        String response = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/meetings"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createThenFetchMeeting() throws Exception {
        String token = signupAndGetToken("owner1@b.com");
        String body = objectMapper.writeValueAsString(Map.of(
            "title", "Sprint Planning",
            "startTime", "2026-07-15T09:00:00",
            "endTime", "2026-07-15T10:00:00",
            "timezone", "Europe/London",
            "recurrence", "WEEKLY",
            "participants", new Object[]{Map.of("name", "Carol", "email", "c@b.com")}));

        String created = mockMvc.perform(post("/api/meetings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.durationMinutes").value(60))
            .andExpect(jsonPath("$.participantCount").value(1))
            .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();
        mockMvc.perform(get("/api/meetings/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Sprint Planning"));
    }

    @Test
    void endBeforeStartReturns400() throws Exception {
        String token = signupAndGetToken("owner2@b.com");
        String body = objectMapper.writeValueAsString(Map.of(
            "title", "Bad",
            "startTime", "2026-07-15T10:00:00",
            "endTime", "2026-07-15T09:00:00",
            "timezone", "Europe/London"));
        mockMvc.perform(post("/api/meetings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }
}
