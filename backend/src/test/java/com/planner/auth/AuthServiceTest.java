package com.planner.auth;

import com.planner.auth.dto.LoginRequest;
import com.planner.auth.dto.SignupRequest;
import com.planner.security.JwtService;
import com.planner.user.User;
import com.planner.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository repo;
    private AuthService service;

    @BeforeEach
    void setup() {
        repo = mock(UserRepository.class);
        Map<String, User> store = new HashMap<>();
        when(repo.existsByEmail(anyString()))
            .thenAnswer(i -> store.containsKey(i.getArgument(0)));
        when(repo.findByEmail(anyString()))
            .thenAnswer(i -> Optional.ofNullable(store.get(i.getArgument(0))));
        when(repo.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            store.put(u.getEmail(), u);
            return u;
        });
        JwtService jwt = new JwtService(
            "local-dev-secret-change-me-please-make-it-long-enough-256bit", 86400000L);
        service = new AuthService(repo, new BCryptPasswordEncoder(), jwt);
    }

    @Test
    void signupHashesPasswordAndReturnsToken() {
        String token = service.signup(
            new SignupRequest("a@b.com", "secret", "Alice")).token();
        assertNotNull(token);
        User saved = repo.findByEmail("a@b.com").orElseThrow();
        assertNotEquals("secret", saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2"));
    }

    @Test
    void duplicateEmailRejected() {
        service.signup(new SignupRequest("a@b.com", "secret", "Alice"));
        assertThrows(IllegalArgumentException.class,
            () -> service.signup(new SignupRequest("a@b.com", "x", "Bob")));
    }

    @Test
    void loginWrongPasswordFails() {
        service.signup(new SignupRequest("a@b.com", "secret", "Alice"));
        assertThrows(IllegalArgumentException.class,
            () -> service.login(new LoginRequest("a@b.com", "wrong")));
    }

    @Test
    void loginCorrectPasswordReturnsToken() {
        service.signup(new SignupRequest("a@b.com", "secret", "Alice"));
        assertNotNull(service.login(new LoginRequest("a@b.com", "secret")).token());
    }
}
