package com.stocksim.user;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * HTTP layer for users: request validation and JSON mapping. Business rules live in {@link UserService}.
 *
 * POST /api/users       sign up (public)
 * GET  /api/me          the logged-in user
 * GET  /api/users/{id}  fetch a user (only yourself)
 */
@RestController
@RequestMapping("/api")
public class UserController {

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 100) String displayName,
            @Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter currency code like EUR") String reportingCurrency) {}

    public record UserResponse(UUID id, String email, String displayName, String status,
                               String reportingCurrency, Instant createdAt) {
        static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getStatus().name(),
                    u.getReportingCurrency(), u.getCreatedAt());
        }
    }

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        User saved = userService.register(req.email(), req.password(), req.displayName(), req.reportingCurrency());
        return ResponseEntity.created(URI.create("/api/users/" + saved.getId())).body(UserResponse.from(saved));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(userService.get(currentUserId(jwt)));
    }

    @GetMapping("/users/{id}")
    public UserResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
         if (!id.equals(currentUserId(jwt))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "you can only view your own user");
        }
        return UserResponse.from(userService.get(id));
    }

    private static UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
