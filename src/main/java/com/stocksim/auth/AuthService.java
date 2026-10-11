package com.stocksim.auth;

import java.time.Instant;
import java.util.Locale;

import com.stocksim.email.EmailSender;
import com.stocksim.user.User;
import com.stocksim.user.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Checks credentials and issues signed access tokens. */
@Service
public class AuthService {

    public record AccessToken(String value, Instant expiresAt) {}

    static final int MAX_FAILED_LOGINS = 3;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;
    private final AuthProperties authProps;
    private final EmailSender emailSender;
    // Hash compared against when the email is unknown, so both failure paths take the same time.
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       JwtProperties props, AuthProperties authProps, EmailSender emailSender) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.authProps = authProps;
        this.emailSender = emailSender;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /**
     * @throws InvalidCredentialsException if the email is unknown, the password is wrong,
     *         or the account is locked or not active (deliberately indistinguishable)
     */
    // noRollbackFor: a wrong password throws, but the failed-attempt count must still be committed.
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AccessToken login(String email, String rawPassword) {
        User user = users.findWithLockByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);

        if (user == null) {
            passwordEncoder.matches(rawPassword, dummyHash);
            throw new InvalidCredentialsException();
        }
        // Always check the password, even when locked, so a locked account answers just as slowly as any other.
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        Instant now = Instant.now();
        if (user.isLocked(now) || user.getStatus() != User.Status.ACTIVE) {
            throw new InvalidCredentialsException();
        }
        if (!passwordMatches) {
            if (user.recordFailedLogin(MAX_FAILED_LOGINS, now.plus(authProps.lockoutDuration()))) {
                emailSender.send(user.getEmail(), "Your stock-sim account has been locked",
                        "After " + MAX_FAILED_LOGINS + " wrong passwords your account is locked for "
                                + authProps.lockoutDuration().toHours() + " hours.\n"
                                + "If this wasn't you, reset your password to unlock it now.");
            }
            throw new InvalidCredentialsException();
        }
        user.recordSuccessfulLogin();
        return issueToken(user);
    }

    private AccessToken issueToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("stock-sim")
                .subject(user.getId().toString())   // "sub": who this token is about
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, expiresAt);
    }
}
