package com.planner.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService service() {
        return new JwtService(
            "local-dev-secret-change-me-please-make-it-long-enough-256bit",
            86400000L);
    }

    @Test
    void generatesAndValidatesToken() {
        JwtService jwt = service();
        String token = jwt.generateToken("user@example.com");
        assertEquals("user@example.com", jwt.extractSubject(token));
        assertTrue(jwt.isValid(token));
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwt = service();
        String token = jwt.generateToken("user@example.com");
        assertFalse(jwt.isValid(token + "x"));
    }
}
