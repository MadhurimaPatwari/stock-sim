package com.stocksim.user;

import java.util.Locale;
import java.util.UUID;

import com.stocksim.currency.CurrencyRepository;
import com.stocksim.currency.UnsupportedCurrencyException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class UserService {

    static final String DEFAULT_CURRENCY = "EUR";

    private final UserRepository users;
    private final CurrencyRepository currencies;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, CurrencyRepository currencies, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.currencies = currencies;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user. Email is stored lowercase; currency defaults to {@value #DEFAULT_CURRENCY}.
     *
     * @throws UnsupportedCurrencyException if the currency is not in the currencies table
     * @throws EmailAlreadyRegisteredException if another user has this email
     */
    @Transactional
    public User register(String email, String rawPassword, String displayName, String reportingCurrency) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String currency = reportingCurrency == null
                ? DEFAULT_CURRENCY
                : reportingCurrency.trim().toUpperCase(Locale.ROOT);

        if (!currencies.existsById(currency)) {
            throw new UnsupportedCurrencyException(currency);
        }
        if (users.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        return users.save(new User(normalizedEmail, passwordEncoder.encode(rawPassword), displayName, currency));
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
