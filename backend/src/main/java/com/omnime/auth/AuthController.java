package com.omnime.auth;

import com.omnime.user.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private final AuthService authService;
        private final JwtService jwtService;
        private final AuthCookieService authCookieService;
        private final CookieCsrfTokenRepository csrfTokenRepository;

        public AuthController(
                        AuthService authService,
                        JwtService jwtService,
                        AuthCookieService authCookieService,
                        CookieCsrfTokenRepository csrfTokenRepository) {
                this.authService = authService;
                this.jwtService = jwtService;
                this.authCookieService = authCookieService;
                this.csrfTokenRepository = csrfTokenRepository;
        }

        @PostMapping("/register")
        public ResponseEntity<AuthUserResponse> register(
                        @Valid @RequestBody RegisterRequest request,
                        HttpServletRequest httpRequest,
                        HttpServletResponse httpResponse) {

                User user = authService.register(request);

                String token = jwtService.generateToken(user);

                AuthUserResponse response = new AuthUserResponse(
                                user.getId(),
                                user.getEmail(),
                                user.getDisplayName(),
                                user.isOnboardingCompleted(),
                                user.isIncludeAdultAnime());

                csrfTokenRepository.saveToken(
                                null,
                                httpRequest,
                                httpResponse);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .header(
                                                HttpHeaders.SET_COOKIE,
                                                authCookieService.createAuthCookie(token).toString())
                                .body(response);
        }

        @GetMapping("/csrf")
        public CsrfToken csrf(CsrfToken csrfToken) {
                return csrfToken;
        }

        @PostMapping("/login")
        public ResponseEntity<AuthUserResponse> login(
                        @Valid @RequestBody LoginRequest request,
                        HttpServletRequest httpRequest,
                        HttpServletResponse httpResponse) {

                User user = authService.login(request);

                String token = jwtService.generateToken(user);

                AuthUserResponse response = new AuthUserResponse(
                                user.getId(),
                                user.getEmail(),
                                user.getDisplayName(),
                                user.isOnboardingCompleted(),
                                user.isIncludeAdultAnime());

                csrfTokenRepository.saveToken(
                                null,
                                httpRequest,
                                httpResponse);

                return ResponseEntity.ok()
                                .header(
                                                HttpHeaders.SET_COOKIE,
                                                authCookieService.createAuthCookie(token).toString())
                                .body(response);
        }

        @GetMapping("/me")
        public AuthUserResponse me(Authentication authentication) {
                UUID userId = (UUID) authentication.getPrincipal();

                return authService.getCurrentUser(userId);
        }

        @PatchMapping("/adult-anime-preference")
        public AuthUserResponse updateAdultAnimePreference(
                        Authentication authentication,
                        @RequestBody UpdateAdultAnimePreferenceRequest request) {

                UUID userId = (UUID) authentication.getPrincipal();

                return authService.updateAdultAnimePreference(userId, request);
        }

        @PostMapping("/logout")
        public ResponseEntity<Void> logout(
                        HttpServletRequest httpRequest,
                        HttpServletResponse httpResponse) {

                csrfTokenRepository.saveToken(
                                null,
                                httpRequest,
                                httpResponse);

                return ResponseEntity.noContent()
                                .header(
                                                HttpHeaders.SET_COOKIE,
                                                authCookieService.clearAuthCookie().toString())
                                .build();
        }

        @PostMapping("/forgot-password")
        public ForgotPasswordResponse forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequest request) {
                authService.createPasswordResetToken(request);

                return new ForgotPasswordResponse(
                                "If an account exists for that email, password reset instructions have been sent.");
        }

        @PostMapping("/reset-password")
        public ResponseEntity<Void> resetPassword(
                        @Valid @RequestBody ResetPasswordRequest request) {
                authService.resetPassword(request);

                return ResponseEntity.noContent().build();
        }
}