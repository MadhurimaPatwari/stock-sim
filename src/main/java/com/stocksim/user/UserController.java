package com.stocksim.user;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.stocksim.currency.CurrencyRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Minimal user endpoints, mainly to check the app and database are wired up.
 *
 * POST /api/users       create a user
 * GET  /api/users/{id}  fetch a user
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    public record CreateUserRequest(String email, String password, String displayName, String reportingCurrency) {}

    public record UserResponse(UUID id, String email, String displayName, String status,
                               String reportingCurrency, Instant createdAt) {
        static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getStatus().name(),
                    u.getReportingCurrency(), u.getCreatedAt());
        }
    }

    private final UserRepository users;
    private final CurrencyRepository currencies;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserController(UserRepository users, CurrencyRepository currencies) {
        this.users = users;
        this.currencies = currencies;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody CreateUserRequest req) {
        if (req.email() == null || !req.email().contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
        if (req.password() == null || req.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must be at least 8 characters");
        }

        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String currency = req.reportingCurrency() == null ? "EUR" : req.reportingCurrency().trim().toUpperCase(Locale.ROOT);
        if (!currencies.existsById(currency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "unsupported reportingCurrency '" + currency + "'; see GET /api/currencies");
        }
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }

        User saved = users.save(new User(email, passwordEncoder.encode(req.password()), req.displayName(), currency));
        return ResponseEntity.created(URI.create("/api/users/" + saved.getId())).body(UserResponse.from(saved));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return users.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }
}
