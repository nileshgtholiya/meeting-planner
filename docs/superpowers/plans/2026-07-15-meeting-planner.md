# Meeting Planner Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local meeting planner (JWT auth, avatar upload, meeting CRUD + detail) with Spring Boot + H2 backend and Angular frontend.

**Architecture:** Monorepo with `backend/` (Spring Boot REST API, layered Controller->Service->Repository over H2) and `frontend/` (Angular standalone components, JWT stored in localStorage, HTTP interceptor + route guard). Backend on :8080, Angular dev server on :4200 proxying `/api`.

**Tech Stack:** Java 17, Spring Boot 3.x (Web, Security, Data JPA, Validation), H2 (file mode), jjwt, Maven, JUnit 5 + MockMvc; Angular 17+ (standalone), TypeScript, RxJS, Jasmine/Karma.

**Global rule:** No comments in any source file (explicit requirement).

---

## Backend File Structure

```
backend/
  pom.xml
  src/main/resources/application.yml
  src/main/java/com/planner/
    MeetingPlannerApplication.java
    config/SecurityConfig.java
    config/FileStorageProperties.java
    security/JwtService.java
    security/JwtAuthFilter.java
    web/GlobalExceptionHandler.java
    user/User.java
    user/UserRepository.java
    auth/AuthService.java
    auth/AuthController.java
    auth/dto/*.java
    user/MeController.java
    meeting/Meeting.java
    meeting/Participant.java
    meeting/Recurrence.java
    meeting/MeetingStatus.java
    meeting/MeetingRepository.java
    meeting/MeetingService.java
    meeting/MeetingController.java
    meeting/dto/*.java
    file/FileStorageService.java
    file/FileController.java
  src/test/java/com/planner/
    meeting/MeetingServiceTest.java
    auth/AuthServiceTest.java
    security/JwtServiceTest.java
    meeting/MeetingControllerTest.java
    file/FileStorageServiceTest.java
```

## Frontend File Structure

```
frontend/
  package.json, angular.json, tsconfig*.json, proxy.conf.json
  src/app/
    app.config.ts
    app.routes.ts
    app.component.ts
    core/models.ts
    core/auth.service.ts
    core/auth.interceptor.ts
    core/auth.guard.ts
    core/api.service.ts
    pages/login/login.component.ts
    pages/meetings-list/meetings-list.component.ts
    pages/meeting-form/meeting-form.component.ts
    pages/meeting-detail/meeting-detail.component.ts
    pages/profile/profile.component.ts
  src/app/core/auth.service.spec.ts
  src/app/core/auth.guard.spec.ts
  src/app/pages/login/login.component.spec.ts
```

---

## Task 0: Repo init

**Files:** Create `.gitignore`.

- [ ] **Step 1:** `git init` in `P:\Task`.
- [ ] **Step 2:** Create `.gitignore`:

```
target/
node_modules/
uploads/
*.mv.db
*.trace.db
.angular/
dist/
```

- [ ] **Step 3:** Commit.

```bash
git add .gitignore docs
git commit -m "chore: init repo, add gitignore and design docs"
```

---

## Task 1: Backend scaffold

**Files:** Create `backend/pom.xml`, `MeetingPlannerApplication.java`, `application.yml`.

- [ ] **Step 1:** `pom.xml` — Spring Boot 3.3.x parent, Java 17. Dependencies: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `com.h2database:h2`, `io.jsonwebtoken:jjwt-api/impl/jackson:0.12.6`, `spring-boot-starter-test`, `spring-security-test`.
- [ ] **Step 2:** `MeetingPlannerApplication.java`:

```java
package com.planner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MeetingPlannerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingPlannerApplication.class, args);
    }
}
```

- [ ] **Step 3:** `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/planner;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
  h2:
    console:
      enabled: true
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 2MB
app:
  jwt:
    secret: "local-dev-secret-change-me-please-make-it-long-enough-256bit"
    expiration-ms: 86400000
  storage:
    dir: ./uploads
```

- [ ] **Step 4:** Run `./mvnw -q compile`. Expected: BUILD SUCCESS.
- [ ] **Step 5:** Commit `feat: scaffold spring boot backend`.

---

## Task 2: User entity + repository

**Files:** Create `user/User.java`, `user/UserRepository.java`.

- [ ] **Step 1:** `User.java`:

```java
package com.planner.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String displayName;

    private String avatarPath;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String v) { this.avatarPath = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
```

- [ ] **Step 2:** `UserRepository.java`:

```java
package com.planner.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 3:** Commit `feat: add user entity and repository`.

---

## Task 3: JWT service (TDD)

**Files:** Create `security/JwtService.java`, `src/test/.../security/JwtServiceTest.java`.

- [ ] **Step 1: Write failing test** `JwtServiceTest.java`:

```java
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
```

- [ ] **Step 2:** Run `./mvnw -q test -Dtest=JwtServiceTest`. Expected: FAIL (JwtService missing).
- [ ] **Step 3:** Implement `JwtService.java`:

```java
package com.planner.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractSubject(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 4:** Run `./mvnw -q test -Dtest=JwtServiceTest`. Expected: PASS.
- [ ] **Step 5:** Commit `feat: add jwt service with tests`.

---

## Task 4: Security config + JWT filter

**Files:** Create `security/JwtAuthFilter.java`, `config/SecurityConfig.java`.

- [ ] **Step 1:** `JwtAuthFilter.java`:

```java
package com.planner.security;

import com.planner.user.User;
import com.planner.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)) {
                String email = jwtService.extractSubject(token);
                Optional<User> user = userRepository.findByEmail(email);
                if (user.isPresent()
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    user.get(), null,
                                    AuthorityUtils.NO_AUTHORITIES);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2:** `SecurityConfig.java`:

```java
package com.planner.config;

import com.planner.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**", "/api/files/**", "/h2-console/**").permitAll()
                .anyRequest().authenticated())
            .headers(h -> h.frameOptions(f -> f.disable()))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 3:** Run `./mvnw -q compile`. Expected: SUCCESS.
- [ ] **Step 4:** Commit `feat: add security config and jwt filter`.

---

## Task 5: Auth service + controller (TDD)

**Files:** Create `auth/dto/SignupRequest.java`, `auth/dto/LoginRequest.java`, `auth/dto/AuthResponse.java`, `auth/AuthService.java`, `auth/AuthController.java`, `src/test/.../auth/AuthServiceTest.java`.

- [ ] **Step 1:** DTOs:

```java
package com.planner.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    @NotBlank String displayName) {}
```

```java
package com.planner.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password) {}
```

```java
package com.planner.auth.dto;

public record AuthResponse(String token) {}
```

- [ ] **Step 2: Write failing test** `AuthServiceTest.java`:

```java
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
```

- [ ] **Step 3:** Run `./mvnw -q test -Dtest=AuthServiceTest`. Expected: FAIL.
- [ ] **Step 4:** Implement `AuthService.java`:

```java
package com.planner.auth;

import com.planner.auth.dto.AuthResponse;
import com.planner.auth.dto.LoginRequest;
import com.planner.auth.dto.SignupRequest;
import com.planner.security.JwtService;
import com.planner.user.User;
import com.planner.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user.getEmail()));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return new AuthResponse(jwtService.generateToken(user.getEmail()));
    }
}
```

- [ ] **Step 5:** Run `./mvnw -q test -Dtest=AuthServiceTest`. Expected: PASS.
- [ ] **Step 6:** Implement `AuthController.java`:

```java
package com.planner.auth;

import com.planner.auth.dto.AuthResponse;
import com.planner.auth.dto.LoginRequest;
import com.planner.auth.dto.SignupRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
```

- [ ] **Step 7:** Commit `feat: add auth service and controller with tests`.

---

## Task 6: Global exception handler

**Files:** Create `web/GlobalExceptionHandler.java`.

- [ ] **Step 1:** Implement:

```java
package com.planner.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(SecurityException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(java.util.NoSuchElementException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, Object fieldErrors) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        if (fieldErrors != null) body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
```

- [ ] **Step 2:** Commit `feat: add global exception handler`.

---

## Task 7: Meeting domain (enums, entities)

**Files:** Create `meeting/Recurrence.java`, `meeting/MeetingStatus.java`, `meeting/Meeting.java`, `meeting/Participant.java`, `meeting/MeetingRepository.java`.

- [ ] **Step 1:** Enums:

```java
package com.planner.meeting;

public enum Recurrence { NONE, DAILY, WEEKLY, MONTHLY }
```

```java
package com.planner.meeting;

public enum MeetingStatus { SCHEDULED, CANCELLED, COMPLETED }
```

- [ ] **Step 2:** `Participant.java`:

```java
package com.planner.meeting;

import jakarta.persistence.*;

@Entity
@Table(name = "participants")
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
}
```

- [ ] **Step 3:** `Meeting.java`:

```java
package com.planner.meeting;

import com.planner.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meetings")
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String location;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recurrence recurrence = Recurrence.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "meeting_id")
    private List<Participant> participants = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime v) { this.startTime = v; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime v) { this.endTime = v; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String v) { this.timezone = v; }
    public Recurrence getRecurrence() { return recurrence; }
    public void setRecurrence(Recurrence v) { this.recurrence = v; }
    public MeetingStatus getStatus() { return status; }
    public void setStatus(MeetingStatus v) { this.status = v; }
    public User getOwner() { return owner; }
    public void setOwner(User v) { this.owner = v; }
    public List<Participant> getParticipants() { return participants; }
    public void setParticipants(List<Participant> v) { this.participants = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
```

- [ ] **Step 4:** `MeetingRepository.java`:

```java
package com.planner.meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByOwnerIdOrderByStartTimeAsc(Long ownerId);
}
```

- [ ] **Step 5:** Commit `feat: add meeting domain entities and repository`.

---

## Task 8: Meeting DTOs

**Files:** Create `meeting/dto/ParticipantDto.java`, `meeting/dto/CreateMeetingRequest.java`, `meeting/dto/MeetingResponse.java`, `meeting/dto/UpdateStatusRequest.java`.

- [ ] **Step 1:** `ParticipantDto.java`:

```java
package com.planner.meeting.dto;

import jakarta.validation.constraints.NotBlank;

public record ParticipantDto(@NotBlank String name, String email) {}
```

- [ ] **Step 2:** `CreateMeetingRequest.java`:

```java
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
```

- [ ] **Step 3:** `UpdateStatusRequest.java`:

```java
package com.planner.meeting.dto;

import com.planner.meeting.MeetingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull MeetingStatus status) {}
```

- [ ] **Step 4:** `MeetingResponse.java`:

```java
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
```

- [ ] **Step 5:** Commit `feat: add meeting dtos`.

---

## Task 9: Meeting service (TDD — core business logic)

**Files:** Create `meeting/MeetingService.java`, `src/test/.../meeting/MeetingServiceTest.java`.

- [ ] **Step 1: Write failing test** `MeetingServiceTest.java`:

```java
package com.planner.meeting;

import com.planner.meeting.dto.CreateMeetingRequest;
import com.planner.meeting.dto.MeetingResponse;
import com.planner.meeting.dto.ParticipantDto;
import com.planner.user.User;
import com.planner.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
```

- [ ] **Step 2:** Run `./mvnw -q test -Dtest=MeetingServiceTest`. Expected: FAIL.
- [ ] **Step 3:** Implement `MeetingService.java`:

```java
package com.planner.meeting;

import com.planner.meeting.dto.CreateMeetingRequest;
import com.planner.meeting.dto.MeetingResponse;
import com.planner.meeting.dto.ParticipantDto;
import com.planner.user.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
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
```

- [ ] **Step 4:** Run `./mvnw -q test -Dtest=MeetingServiceTest`. Expected: PASS.
- [ ] **Step 5:** Commit `feat: add meeting service with business-logic tests`.

---

## Task 10: Meeting controller + MeController + current-user resolution

**Files:** Create `meeting/MeetingController.java`, `user/MeController.java`, `user/dto/UserResponse.java`.

- [ ] **Step 1:** `user/dto/UserResponse.java`:

```java
package com.planner.user.dto;

import com.planner.user.User;

public record UserResponse(Long id, String email, String displayName, String avatarUrl) {
    public static UserResponse from(User u) {
        String url = u.getAvatarPath() == null ? null
            : "/api/files/avatars/" + u.getAvatarPath();
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), url);
    }
}
```

- [ ] **Step 2:** `MeetingController.java`:

```java
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
```

- [ ] **Step 3:** `MeController.java`:

```java
package com.planner.user;

import com.planner.user.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }
}
```

- [ ] **Step 4:** Run `./mvnw -q compile`. Expected: SUCCESS.
- [ ] **Step 5:** Commit `feat: add meeting and me controllers`.

---

## Task 11: File storage service + avatar endpoints (TDD)

**Files:** Create `file/FileStorageService.java`, `file/FileController.java`, add avatar endpoint to `MeController`, `src/test/.../file/FileStorageServiceTest.java`.

- [ ] **Step 1: Write failing test** `FileStorageServiceTest.java`:

```java
package com.planner.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @Test
    void storesValidImageAndReturnsFilename(@TempDir Path dir) {
        FileStorageService service = new FileStorageService(dir.toString());
        MockMultipartFile file = new MockMultipartFile(
            "file", "a.png", "image/png", new byte[]{1, 2, 3});
        String name = service.storeAvatar(file);
        assertNotNull(name);
        assertTrue(name.endsWith(".png"));
    }

    @Test
    void rejectsNonImage(@TempDir Path dir) {
        FileStorageService service = new FileStorageService(dir.toString());
        MockMultipartFile file = new MockMultipartFile(
            "file", "a.txt", "text/plain", new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> service.storeAvatar(file));
    }

    @Test
    void rejectsEmptyFile(@TempDir Path dir) {
        FileStorageService service = new FileStorageService(dir.toString());
        MockMultipartFile file = new MockMultipartFile(
            "file", "a.png", "image/png", new byte[]{});
        assertThrows(IllegalArgumentException.class, () -> service.storeAvatar(file));
    }
}
```

- [ ] **Step 2:** Run `./mvnw -q test -Dtest=FileStorageServiceTest`. Expected: FAIL.
- [ ] **Step 3:** Implement `FileStorageService.java`:

```java
package com.planner.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Map<String, String> ALLOWED = Map.of(
        "image/png", ".png",
        "image/jpeg", ".jpg");

    private final Path avatarDir;

    public FileStorageService(@Value("${app.storage.dir}") String storageDir) {
        this.avatarDir = Paths.get(storageDir, "avatars");
        try {
            Files.createDirectories(this.avatarDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String extension = ALLOWED.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Only PNG and JPEG images are allowed");
        }
        String filename = UUID.randomUUID() + extension;
        try {
            Files.copy(file.getInputStream(), avatarDir.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return filename;
    }

    public Resource loadAvatar(String filename) {
        try {
            Path file = avatarDir.resolve(filename).normalize();
            if (!file.startsWith(avatarDir)) {
                throw new IllegalArgumentException("Invalid path");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) {
                throw new java.util.NoSuchElementException("File not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid path", e);
        }
    }
}
```

- [ ] **Step 4:** Run `./mvnw -q test -Dtest=FileStorageServiceTest`. Expected: PASS.
- [ ] **Step 5:** Add avatar endpoint to `MeController.java` (inject `FileStorageService` + `UserRepository`):

```java
package com.planner.user;

import com.planner.file.FileStorageService;
import com.planner.user.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public MeController(FileStorageService fileStorageService, UserRepository userRepository) {
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }

    @PostMapping("/avatar")
    public UserResponse uploadAvatar(@AuthenticationPrincipal User user,
                                     @RequestParam("file") MultipartFile file) {
        String filename = fileStorageService.storeAvatar(file);
        user.setAvatarPath(filename);
        userRepository.save(user);
        return UserResponse.from(user);
    }
}
```

- [ ] **Step 6:** Implement `FileController.java`:

```java
package com.planner.file;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/avatars/{filename}")
    public ResponseEntity<Resource> avatar(@PathVariable String filename) {
        Resource resource = fileStorageService.loadAvatar(filename);
        String contentType = filename.endsWith(".png") ? "image/png" : "image/jpeg";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(resource);
    }
}
```

- [ ] **Step 7:** Run `./mvnw -q test`. Expected: PASS.
- [ ] **Step 8:** Commit `feat: add avatar upload and file serving with tests`.

---

## Task 12: Meeting controller integration test (MockMvc, TDD)

**Files:** Create `src/test/.../meeting/MeetingControllerTest.java`.

- [ ] **Step 1: Write test** (full `@SpringBootTest` + `@AutoConfigureMockMvc`):

```java
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
```

- [ ] **Step 2:** Run `./mvnw -q test -Dtest=MeetingControllerTest`. Expected: PASS.
- [ ] **Step 3:** Run full suite `./mvnw -q test`. Expected: PASS.
- [ ] **Step 4:** Commit `test: add meeting controller integration tests`.

---

## Task 13: Frontend scaffold

**Files:** Create Angular workspace in `frontend/` (standalone, routing, no SSR). Create `proxy.conf.json`.

- [ ] **Step 1:** `npx @angular/cli@17 new frontend --standalone --routing --style=css --skip-git --ssr=false` (run inside `P:\Task`).
- [ ] **Step 2:** `proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false
  }
}
```

- [ ] **Step 3:** In `angular.json`, set `serve.options.proxyConfig` to `"proxy.conf.json"` (under the project's `architect.serve.options`).
- [ ] **Step 4:** `npm start` briefly to confirm boot, then stop. Commit `feat: scaffold angular frontend with proxy`.

---

## Task 14: Frontend models + core services

**Files:** Create `core/models.ts`, `core/auth.service.ts`, `core/api.service.ts`, `core/auth.interceptor.ts`, `core/auth.guard.ts`. Modify `app.config.ts`.

- [ ] **Step 1:** `models.ts`:

```typescript
export type Recurrence = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY';
export type MeetingStatus = 'SCHEDULED' | 'CANCELLED' | 'COMPLETED';

export interface Participant {
  name: string;
  email?: string;
}

export interface User {
  id: number;
  email: string;
  displayName: string;
  avatarUrl?: string;
}

export interface Meeting {
  id: number;
  title: string;
  description?: string;
  location?: string;
  startTime: string;
  endTime: string;
  timezone: string;
  recurrence: Recurrence;
  status: MeetingStatus;
  ownerDisplayName: string;
  participants: Participant[];
  durationMinutes: number;
  participantCount: number;
}

export interface CreateMeeting {
  title: string;
  description?: string;
  location?: string;
  startTime: string;
  endTime: string;
  timezone: string;
  recurrence: Recurrence;
  participants: Participant[];
}
```

- [ ] **Step 2:** `auth.service.ts`:

```typescript
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

const TOKEN_KEY = 'mp_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));

  constructor(private http: HttpClient) {}

  get token(): string | null {
    return this.tokenSignal();
  }

  isLoggedIn(): boolean {
    return this.tokenSignal() !== null;
  }

  signup(email: string, password: string, displayName: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>('/api/auth/signup',
      { email, password, displayName }).pipe(tap(r => this.store(r.token)));
  }

  login(email: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>('/api/auth/login',
      { email, password }).pipe(tap(r => this.store(r.token)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.tokenSignal.set(null);
  }

  private store(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.tokenSignal.set(token);
  }
}
```

- [ ] **Step 3:** `api.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateMeeting, Meeting, MeetingStatus, User } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  me(): Observable<User> {
    return this.http.get<User>('/api/me');
  }

  uploadAvatar(file: File): Observable<User> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<User>('/api/me/avatar', form);
  }

  listMeetings(): Observable<Meeting[]> {
    return this.http.get<Meeting[]>('/api/meetings');
  }

  createMeeting(payload: CreateMeeting): Observable<Meeting> {
    return this.http.post<Meeting>('/api/meetings', payload);
  }

  getMeeting(id: number): Observable<Meeting> {
    return this.http.get<Meeting>(`/api/meetings/${id}`);
  }

  updateStatus(id: number, status: MeetingStatus): Observable<Meeting> {
    return this.http.patch<Meeting>(`/api/meetings/${id}/status`, { status });
  }
}
```

- [ ] **Step 4:** `auth.interceptor.ts`:

```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token;
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;
  return next(authReq).pipe(
    catchError((error) => {
      if (error.status === 401) {
        auth.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
```

- [ ] **Step 5:** `auth.guard.ts`:

```typescript
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) {
    return true;
  }
  router.navigate(['/login']);
  return false;
};
```

- [ ] **Step 6:** `app.config.ts` — provide HttpClient with interceptor:

```typescript
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
```

- [ ] **Step 7:** Commit `feat: add frontend core services, interceptor, guard`.

---

## Task 15: Frontend routes + app shell

**Files:** Modify `app.routes.ts`, `app.component.ts`.

- [ ] **Step 1:** `app.routes.ts`:

```typescript
import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'meetings' },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'meetings',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/meetings-list/meetings-list.component').then(m => m.MeetingsListComponent)
  },
  {
    path: 'meetings/new',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/meeting-form/meeting-form.component').then(m => m.MeetingFormComponent)
  },
  {
    path: 'meetings/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/meeting-detail/meeting-detail.component').then(m => m.MeetingDetailComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent)
  },
  { path: '**', redirectTo: 'meetings' }
];
```

- [ ] **Step 2:** `app.component.ts` (nav bar + router outlet):

```typescript
import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <nav>
      @if (auth.isLoggedIn()) {
        <a routerLink="/meetings">Meetings</a>
        <a routerLink="/meetings/new">New</a>
        <a routerLink="/profile">Profile</a>
        <button (click)="logout()">Logout</button>
      }
    </nav>
    <main><router-outlet /></main>
  `,
  styles: [`
    nav { display: flex; gap: 12px; padding: 12px; border-bottom: 1px solid #ddd; }
    main { padding: 16px; max-width: 800px; }
  `]
})
export class AppComponent {
  constructor(public auth: AuthService, private router: Router) {}

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
```

- [ ] **Step 3:** Commit `feat: add frontend routes and app shell`.

---

## Task 16: Login page

**Files:** Create `pages/login/login.component.ts`.

- [ ] **Step 1:** Implement:

```typescript
import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <h2>{{ mode === 'login' ? 'Log In' : 'Sign Up' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      @if (mode === 'signup') {
        <label>Display name<input formControlName="displayName" /></label>
      }
      <label>Email<input type="email" formControlName="email" /></label>
      <label>Password<input type="password" formControlName="password" /></label>
      <button type="submit" [disabled]="form.invalid">
        {{ mode === 'login' ? 'Log In' : 'Sign Up' }}
      </button>
    </form>
    @if (error) { <p class="error">{{ error }}</p> }
    <button type="button" (click)="toggle()">
      {{ mode === 'login' ? 'Need an account? Sign up' : 'Have an account? Log in' }}
    </button>
  `,
  styles: [`
    form { display: flex; flex-direction: column; gap: 8px; max-width: 320px; }
    label { display: flex; flex-direction: column; }
    .error { color: #c00; }
  `]
})
export class LoginComponent {
  mode: 'login' | 'signup' = 'login';
  error = '';
  form = this.fb.group({
    displayName: [''],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  toggle(): void {
    this.mode = this.mode === 'login' ? 'signup' : 'login';
    this.error = '';
  }

  submit(): void {
    this.error = '';
    const { email, password, displayName } = this.form.value;
    const request = this.mode === 'login'
      ? this.auth.login(email!, password!)
      : this.auth.signup(email!, password!, displayName || 'User');
    request.subscribe({
      next: () => this.router.navigate(['/meetings']),
      error: (e) => this.error = e.error?.message || 'Request failed'
    });
  }
}
```

- [ ] **Step 2:** Commit `feat: add login/signup page`.

---

## Task 17: Meetings list page

**Files:** Create `pages/meetings-list/meetings-list.component.ts`.

- [ ] **Step 1:** Implement:

```typescript
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { Meeting } from '../../core/models';

@Component({
  selector: 'app-meetings-list',
  standalone: true,
  imports: [RouterLink, DatePipe],
  template: `
    <h2>My Meetings</h2>
    @if (meetings.length === 0) {
      <p>No meetings yet. <a routerLink="/meetings/new">Create one</a>.</p>
    } @else {
      <table>
        <thead>
          <tr><th>Title</th><th>Start</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
          @for (m of meetings; track m.id) {
            <tr>
              <td>{{ m.title }}</td>
              <td>{{ m.startTime | date:'short' }}</td>
              <td>{{ m.status }}</td>
              <td><a [routerLink]="['/meetings', m.id]">View</a></td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
  styles: [`
    table { border-collapse: collapse; width: 100%; }
    th, td { text-align: left; padding: 6px 10px; border-bottom: 1px solid #eee; }
  `]
})
export class MeetingsListComponent implements OnInit {
  meetings: Meeting[] = [];

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.listMeetings().subscribe(m => this.meetings = m);
  }
}
```

- [ ] **Step 2:** Commit `feat: add meetings list page`.

---

## Task 18: Meeting form page

**Files:** Create `pages/meeting-form/meeting-form.component.ts`.

- [ ] **Step 1:** Implement:

```typescript
import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormArray, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { CreateMeeting } from '../../core/models';

@Component({
  selector: 'app-meeting-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <h2>New Meeting</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label>Title<input formControlName="title" /></label>
      <label>Description<textarea formControlName="description"></textarea></label>
      <label>Location<input formControlName="location" /></label>
      <label>Start<input type="datetime-local" formControlName="startTime" /></label>
      <label>End<input type="datetime-local" formControlName="endTime" /></label>
      <label>Timezone<input formControlName="timezone" /></label>
      <label>Recurrence
        <select formControlName="recurrence">
          <option value="NONE">None</option>
          <option value="DAILY">Daily</option>
          <option value="WEEKLY">Weekly</option>
          <option value="MONTHLY">Monthly</option>
        </select>
      </label>

      <h3>Participants</h3>
      <div formArrayName="participants">
        @for (p of participants.controls; track $index) {
          <div [formGroupName]="$index" class="participant">
            <input placeholder="Name" formControlName="name" />
            <input placeholder="Email" formControlName="email" />
            <button type="button" (click)="removeParticipant($index)">Remove</button>
          </div>
        }
      </div>
      <button type="button" (click)="addParticipant()">Add participant</button>

      <button type="submit" [disabled]="form.invalid">Create</button>
    </form>
    @if (error) { <p class="error">{{ error }}</p> }
  `,
  styles: [`
    form { display: flex; flex-direction: column; gap: 8px; max-width: 480px; }
    label { display: flex; flex-direction: column; }
    .participant { display: flex; gap: 6px; margin-bottom: 6px; }
    .error { color: #c00; }
  `]
})
export class MeetingFormComponent {
  error = '';
  form = this.fb.group({
    title: ['', Validators.required],
    description: [''],
    location: [''],
    startTime: ['', Validators.required],
    endTime: ['', Validators.required],
    timezone: [Intl.DateTimeFormat().resolvedOptions().timeZone, Validators.required],
    recurrence: ['NONE', Validators.required],
    participants: this.fb.array([])
  });

  constructor(private fb: FormBuilder, private api: ApiService, private router: Router) {}

  get participants(): FormArray {
    return this.form.get('participants') as FormArray;
  }

  addParticipant(): void {
    this.participants.push(this.fb.group({
      name: ['', Validators.required],
      email: ['']
    }));
  }

  removeParticipant(index: number): void {
    this.participants.removeAt(index);
  }

  submit(): void {
    this.error = '';
    const value = this.form.value;
    const payload: CreateMeeting = {
      title: value.title!,
      description: value.description || undefined,
      location: value.location || undefined,
      startTime: value.startTime!,
      endTime: value.endTime!,
      timezone: value.timezone!,
      recurrence: value.recurrence as CreateMeeting['recurrence'],
      participants: (value.participants as { name: string; email?: string }[]) || []
    };
    this.api.createMeeting(payload).subscribe({
      next: (m) => this.router.navigate(['/meetings', m.id]),
      error: (e) => this.error = e.error?.message || 'Could not create meeting'
    });
  }
}
```

- [ ] **Step 2:** Commit `feat: add meeting creation form`.

---

## Task 19: Meeting detail page

**Files:** Create `pages/meeting-detail/meeting-detail.component.ts`.

- [ ] **Step 1:** Implement:

```typescript
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { Meeting, MeetingStatus } from '../../core/models';

@Component({
  selector: 'app-meeting-detail',
  standalone: true,
  imports: [DatePipe],
  template: `
    @if (meeting) {
      <h2>{{ meeting.title }}</h2>
      <p><strong>Status:</strong> {{ meeting.status }}</p>
      <p><strong>When:</strong> {{ meeting.startTime | date:'medium' }}
        &ndash; {{ meeting.endTime | date:'shortTime' }} ({{ meeting.timezone }})</p>
      <p><strong>Duration:</strong> {{ meeting.durationMinutes }} minutes</p>
      <p><strong>Recurrence:</strong> {{ meeting.recurrence }}</p>
      <p><strong>Location:</strong> {{ meeting.location || '-' }}</p>
      <p><strong>Owner:</strong> {{ meeting.ownerDisplayName }}</p>
      @if (meeting.description) { <p>{{ meeting.description }}</p> }

      <h3>Participants ({{ meeting.participantCount }})</h3>
      <ul>
        @for (p of meeting.participants; track p.email) {
          <li>{{ p.name }} @if (p.email) { <span>&lt;{{ p.email }}&gt;</span> }</li>
        }
      </ul>

      <label>Change status
        <select [value]="meeting.status" (change)="changeStatus($event)">
          <option value="SCHEDULED">Scheduled</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="COMPLETED">Completed</option>
        </select>
      </label>
    } @else if (error) {
      <p class="error">{{ error }}</p>
    }
  `,
  styles: [`.error { color: #c00; }`]
})
export class MeetingDetailComponent implements OnInit {
  meeting?: Meeting;
  error = '';

  constructor(private route: ActivatedRoute, private api: ApiService) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getMeeting(id).subscribe({
      next: (m) => this.meeting = m,
      error: () => this.error = 'Meeting not found or not accessible'
    });
  }

  changeStatus(event: Event): void {
    const status = (event.target as HTMLSelectElement).value as MeetingStatus;
    this.api.updateStatus(this.meeting!.id, status)
      .subscribe(m => this.meeting = m);
  }
}
```

- [ ] **Step 2:** Commit `feat: add meeting detail page`.

---

## Task 20: Profile page (avatar upload)

**Files:** Create `pages/profile/profile.component.ts`.

- [ ] **Step 1:** Implement:

```typescript
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { User } from '../../core/models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [],
  template: `
    @if (user) {
      <h2>{{ user.displayName }}</h2>
      <p>{{ user.email }}</p>
      @if (user.avatarUrl) {
        <img [src]="user.avatarUrl" alt="avatar" width="120" height="120" />
      }
      <div>
        <input type="file" accept="image/png,image/jpeg" (change)="onFile($event)" />
        <button (click)="upload()" [disabled]="!file">Upload avatar</button>
      </div>
      @if (error) { <p class="error">{{ error }}</p> }
    }
  `,
  styles: [`.error { color: #c00; } img { border-radius: 50%; object-fit: cover; }`]
})
export class ProfileComponent implements OnInit {
  user?: User;
  file?: File;
  error = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.me().subscribe(u => this.user = u);
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = input.files?.[0] || undefined;
  }

  upload(): void {
    if (!this.file) return;
    this.error = '';
    this.api.uploadAvatar(this.file).subscribe({
      next: (u) => { this.user = u; this.file = undefined; },
      error: (e) => this.error = e.error?.message || 'Upload failed'
    });
  }
}
```

- [ ] **Step 2:** Commit `feat: add profile page with avatar upload`.

---

## Task 21: Frontend unit tests

**Files:** Create `core/auth.service.spec.ts`, `core/auth.guard.spec.ts`, `pages/login/login.component.spec.ts`.

- [ ] **Step 1:** `auth.service.spec.ts`:

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('starts logged out', () => {
    expect(service.isLoggedIn()).toBe(false);
  });

  it('stores token on login', () => {
    service.login('a@b.com', 'pw').subscribe();
    httpMock.expectOne('/api/auth/login').flush({ token: 'abc' });
    expect(service.token).toBe('abc');
    expect(service.isLoggedIn()).toBe(true);
  });

  it('clears token on logout', () => {
    service.login('a@b.com', 'pw').subscribe();
    httpMock.expectOne('/api/auth/login').flush({ token: 'abc' });
    service.logout();
    expect(service.isLoggedIn()).toBe(false);
    expect(localStorage.getItem('mp_token')).toBeNull();
  });
});
```

- [ ] **Step 2:** `auth.guard.spec.ts`:

```typescript
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { runInInjectionContext, EnvironmentInjector } from '@angular/core';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let injector: EnvironmentInjector;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    injector = TestBed.inject(EnvironmentInjector);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('blocks and redirects when logged out', () => {
    const result = runInInjectionContext(injector,
      () => authGuard({} as any, {} as any));
    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('allows when logged in', () => {
    TestBed.inject(AuthService)['store']('token');
    const result = runInInjectionContext(injector,
      () => authGuard({} as any, {} as any));
    expect(result).toBe(true);
  });
});
```

- [ ] **Step 3:** `login.component.spec.ts`:

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [LoginComponent, HttpClientTestingModule, RouterTestingModule]
    });
  });

  it('creates and defaults to login mode', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component.mode).toBe('login');
  });

  it('toggles to signup mode', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance.toggle();
    expect(fixture.componentInstance.mode).toBe('signup');
  });
});
```

- [ ] **Step 4:** Run `npm test -- --watch=false --browsers=ChromeHeadless`. Expected: PASS.
- [ ] **Step 5:** Commit `test: add frontend unit tests`.

---

## Task 22: README + AI_LOG

**Files:** Create `README.md`, `AI_LOG.md`.

- [ ] **Step 1:** `README.md` covering: prerequisites (Java 17, Node 18+), run backend (`cd backend && ./mvnw spring-boot:run`), run frontend (`cd frontend && npm install && npm start`, open http://localhost:4200), run backend tests (`cd backend && ./mvnw test`), run frontend tests (`cd frontend && npm test -- --watch=false --browsers=ChromeHeadless`), design decisions (JWT, free-form participants, H2 file mode, local avatar storage, recurrence as label), assumptions, known limitations (from spec), future improvements.
- [ ] **Step 2:** `AI_LOG.md` with the key prompts used (design brainstorming decisions, plan generation, per-task implementation).
- [ ] **Step 3:** Commit `docs: add README and AI log`.

---

## Task 23: End-to-end manual verification

- [ ] **Step 1:** Start backend + frontend. Sign up, create a meeting with participants, view detail (verify duration + participant count), change status, upload avatar, log out/in.
- [ ] **Step 2:** Run full backend + frontend test suites. Expected: all PASS.
- [ ] **Step 3:** Final commit `chore: final verification pass`.

---

## Self-Review Notes

- Spec coverage: signup/login (T5), avatar upload (T11,T20), create meeting (T9,T18), meeting detail with useful info/duration (T8,T19), participants (T7,T18), H2 (T1), Angular (T13+), tests for core business logic (T3,T5,T9,T11,T12,T21), README+AI_LOG (T22), multiple commits (every task). All covered.
- Type consistency: `MeetingResponse.from`, `storeAvatar`/`loadAvatar`, `getForUser`/`listForUser`/`updateStatus`, `authInterceptor`, `authGuard` names consistent across tasks.
- No placeholders: every code step contains full code.
