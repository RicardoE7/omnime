package com.omnime.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetDeliveryService {

    private final boolean logResetTokens;

    public PasswordResetDeliveryService(
            @Value("${app.password-reset.log-tokens:false}") boolean logResetTokens) {
        this.logResetTokens = logResetTokens;
    }

    public void sendResetToken(String email, String rawToken) {
        if (logResetTokens) {
            System.out.println(
                    "DEV PASSWORD RESET TOKEN for " + email + ": " + rawToken);
        }
    }
}