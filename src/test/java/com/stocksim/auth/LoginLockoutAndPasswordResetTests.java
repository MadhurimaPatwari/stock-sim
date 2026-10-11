package com.stocksim.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import com.stocksim.TestcontainersConfiguration;
import com.stocksim.email.EmailSender;
import com.stocksim.user.UserService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LoginLockoutAndPasswordResetTests {

    static final String PASSWORD = "correct-horse";

    @Autowired UserService userService;
    @Autowired AuthService authService;
    @Autowired PasswordResetService passwordResetService;

    // Replaces the real EmailSender so we can see what would have been sent.
    @MockitoBean EmailSender emailSender;

    @Test
    void threeWrongPasswordsLockTheAccountEvenForTheCorrectPassword() {
        String email = newUser();

        failLogin(email, "wrong-1");
        failLogin(email, "wrong-2");
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
        failLogin(email, "wrong-3");

        verify(emailSender).send(eq(email), eq("Your stock-sim account has been locked"), anyString());
        failLogin(email, PASSWORD);
    }

    @Test
    void successfulLoginResetsTheFailureCount() {
        String email = newUser();

        failLogin(email, "wrong-1");
        failLogin(email, "wrong-2");
        authService.login(email, PASSWORD);
        failLogin(email, "wrong-3");
        failLogin(email, "wrong-4");

        assertThat(authService.login(email, PASSWORD).value()).isNotBlank();
    }

    @Test
    void forgotPasswordForUnknownEmailSendsNothing() {
        passwordResetService.requestReset("nobody-" + UUID.randomUUID() + "@example.com");

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void passwordResetSetsNewPasswordUnlocksAccountAndTokenWorksOnce() {
        String email = newUser();
        failLogin(email, "wrong-1");
        failLogin(email, "wrong-2");
        failLogin(email, "wrong-3");

        passwordResetService.requestReset(email);
        String token = resetTokenFromEmail(email);
        passwordResetService.resetPassword(token, "brand-new-password");

        assertThat(authService.login(email, "brand-new-password").value()).isNotBlank();
        failLogin(email, PASSWORD);
        assertThatThrownBy(() -> passwordResetService.resetPassword(token, "another-password"))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    private String newUser() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        userService.register(email, PASSWORD, null, null);
        return email;
    }

    private void failLogin(String email, String password) {
        assertThatThrownBy(() -> authService.login(email, password)).isInstanceOf(InvalidCredentialsException.class);
    }

    private String resetTokenFromEmail(String email) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(email), eq("Reset your stock-sim password"), body.capture());
        String[] lines = body.getValue().split("\n");
        return lines[lines.length - 1];
    }
}
