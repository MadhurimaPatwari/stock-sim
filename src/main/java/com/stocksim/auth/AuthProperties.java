package com.stocksim.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code stocksim.auth.*} from application.yml. */
@ConfigurationProperties(prefix = "stocksim.auth")
public record AuthProperties(Duration lockoutDuration) {
}
