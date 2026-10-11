package com.stocksim.email;

/** Sends plain-text email. Callers depend on this interface, not on how mail is delivered. */
public interface EmailSender {

    void send(String to, String subject, String body);
}
