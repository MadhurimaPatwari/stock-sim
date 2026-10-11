package com.stocksim.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code stocksim.jwt.*} from application.yml. */
@ConfigurationProperties(prefix = "stocksim.jwt")
public record JwtProperties(String secret, Duration ttl) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("stocksim.jwt.secret must be at least 32 bytes");
        }
    }
}
