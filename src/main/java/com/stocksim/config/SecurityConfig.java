package com.stocksim.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.stocksim.auth.ApiKeyAuthenticationProvider;
import com.stocksim.auth.AuthProperties;
import com.stocksim.auth.JwtProperties;
import com.stocksim.auth.Scopes;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who may call what. Everything needs a valid "Authorization: Bearer <token>" header (a login JWT or an
 * API key) except the public routes listed below; some routes also need a scope.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AuthProperties.class})
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                            ApiKeyAuthenticationProvider apiKeys) throws Exception {
        // Bearer tokens are tried in order: API keys ("ssk_...") first, then login JWTs.
        AuthenticationManager bearerTokens = new ProviderManager(apiKeys, new JwtAuthenticationProvider(jwtDecoder));
        http
                // CSRF protects cookie-based sessions; we use bearer tokens and no cookies, so it is not needed.
                .csrf(csrf -> csrf.disable())
                // Never create an HTTP session: every request must carry its own token.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/users", "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/currencies", "/health").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Spring forwards errors to /error; without this a 400 on a public route would turn into a 401.
                        .requestMatchers("/error").permitAll()
                        // Scopes: first matching rule wins, so these come before anyRequest().
                        .requestMatchers(HttpMethod.POST, "/api/portfolios/*/deposits").hasAuthority(scope(Scopes.PORTFOLIO_DEPOSIT))
                        .requestMatchers(HttpMethod.GET, "/api/portfolios/**").hasAuthority(scope(Scopes.PORTFOLIO_READ))
                        .requestMatchers("/api/api-keys/**").hasAuthority(scope(Scopes.API_KEYS_MANAGE))
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(request -> bearerTokens));
        return http.build();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey(props)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties props) {
        // Checks the signature and that the token has not expired.
        return NimbusJwtDecoder.withSecretKey(signingKey(props)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Spring's name for a scope's authority: "portfolio:read" -> "SCOPE_portfolio:read". */
    private static String scope(String scope) {
        return "SCOPE_" + scope;
    }

    private static SecretKey signingKey(JwtProperties props) {
        return new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
