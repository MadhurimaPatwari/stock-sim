package com.stocksim.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import com.stocksim.email.EmailSender;
import com.stocksim.user.User;
import com.stocksim.user.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder passwordEncoder, EmailSender emailSender) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
    }

    @Transactional
    public void requestReset(String email) {
        users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).ifPresent(user -> {
            String token = Tokens.newSecret();

            tokens.save(new PasswordResetToken(user.getId(), Tokens.sha256(token), Instant.now().plus(TOKEN_TTL)));
            emailSender.send(user.getEmail(), "Reset your stock-sim password",
                    "Use this token with POST /api/auth/reset-password within "
                            + TOKEN_TTL.toMinutes() + " minutes:\n\n" + token);
        });
    }

    /**
     * Sets a new password and unlocks the account.
     *
     * @throws InvalidResetTokenException if the token is unknown, expired or already used
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken resetToken = tokens.findWithLockByTokenHash(Tokens.sha256(token))
                .filter(t -> t.isUsable(now))
                .orElseThrow(InvalidResetTokenException::new);
        User user = users.findById(resetToken.getUserId()).orElseThrow(InvalidResetTokenException::new);

        user.changePassword(passwordEncoder.encode(newPassword));
        resetToken.markUsed(now);
    }
}
