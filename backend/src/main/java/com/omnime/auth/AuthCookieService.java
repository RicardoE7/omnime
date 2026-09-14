package com.omnime.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.http.ResponseCookie;
import java.time.Duration;

@Service
public class AuthCookieService {

    private final boolean secure;
    private final String sameSite;
    private final long expirationMinutes;

    public AuthCookieService(
            @Value("${app.auth.cookie-secure}") boolean secure,
            @Value("${app.auth.cookie-same-site}") String sameSite,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.expirationMinutes = expirationMinutes;
    }

    public ResponseCookie createAuthCookie(String token) {
        return ResponseCookie.from("omnime_auth", token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofMinutes(expirationMinutes))
                .build();
    }

    public ResponseCookie clearAuthCookie() {
        return ResponseCookie.from("omnime_auth", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}