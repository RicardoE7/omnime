package com.omnime.auth;

import com.omnime.common.exception.AuthenticatedUserNotFoundException;
import com.omnime.common.exception.EmailAlreadyRegisteredException;
import com.omnime.common.exception.InvalidCredentialsException;
import com.omnime.common.exception.InvalidPasswordException;
import com.omnime.common.exception.InvalidPasswordResetTokenException;
import com.omnime.user.User;
import com.omnime.user.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

        private UserRepository userRepository;
        private PasswordEncoder passwordEncoder;
        private PasswordResetTokenRepository passwordResetTokenRepository;
        private PasswordResetTokenService passwordResetTokenService;
        private PasswordResetDeliveryService passwordResetDeliveryService;

        private AuthService authService;

        @BeforeEach
        void setUp() {
                userRepository = mock(UserRepository.class);
                passwordEncoder = mock(PasswordEncoder.class);
                passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
                passwordResetTokenService = mock(PasswordResetTokenService.class);
                passwordResetDeliveryService = mock(PasswordResetDeliveryService.class);

                authService = new AuthService(
                                userRepository,
                                passwordEncoder,
                                passwordResetTokenRepository,
                                passwordResetTokenService,
                                passwordResetDeliveryService);
        }

        @Test
        void registerRejectsDuplicateEmail() {
                RegisterRequest request = new RegisterRequest(
                                "test@omnime.com",
                                "Password123",
                                "Test User");

                when(userRepository.existsByEmail("test@omnime.com"))
                                .thenReturn(true);

                assertThrows(
                                EmailAlreadyRegisteredException.class,
                                () -> authService.register(request));
        }

        @Test
        void loginReturnsUserWhenCredentialsAreCorrect() {
                LoginRequest request = new LoginRequest(
                                "TEST@OMNIME.COM",
                                "Password123");

                User user = mock(User.class);

                when(userRepository.findByEmail("test@omnime.com"))
                                .thenReturn(Optional.of(user));

                when(user.getPasswordHash())
                                .thenReturn("stored-password-hash");

                when(passwordEncoder.matches(
                                "Password123",
                                "stored-password-hash"))
                                .thenReturn(true);

                User result = authService.login(request);

                assertSame(user, result);

                verify(userRepository)
                                .findByEmail("test@omnime.com");

                verify(passwordEncoder)
                                .matches("Password123", "stored-password-hash");
        }

        @Test
        void loginRejectsIncorrectPassword() {
                LoginRequest request = new LoginRequest(
                                "test@omnime.com",
                                "WrongPassword123");

                User user = mock(User.class);

                when(userRepository.findByEmail("test@omnime.com"))
                                .thenReturn(Optional.of(user));

                when(user.getPasswordHash())
                                .thenReturn("stored-password-hash");

                when(passwordEncoder.matches(
                                "WrongPassword123",
                                "stored-password-hash"))
                                .thenReturn(false);

                assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.login(request));
        }

        @Test
        void loginRejectsUnknownEmail() {
                LoginRequest request = new LoginRequest(
                                "missing@omnime.com",
                                "Password123");

                when(userRepository.findByEmail("missing@omnime.com"))
                                .thenReturn(Optional.empty());

                assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.login(request));
        }

        @Test
        void registerCreatesUserWithNormalizedData() {
                RegisterRequest request = new RegisterRequest(
                                "  TEST@OMNIME.COM  ",
                                "Password123",
                                "  Test User  ");

                when(userRepository.existsByEmail("test@omnime.com"))
                                .thenReturn(false);

                when(passwordEncoder.encode("Password123"))
                                .thenReturn("hashed-password");

                when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                User result = authService.register(request);

                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

                verify(userRepository).saveAndFlush(userCaptor.capture());

                User savedUser = userCaptor.getValue();

                assertSame(savedUser, result);
                assertEquals("test@omnime.com", savedUser.getEmail());
                assertEquals("hashed-password", savedUser.getPasswordHash());
                assertEquals("Test User", savedUser.getDisplayName());

                verify(passwordEncoder).encode("Password123");
        }

        @Test
        void registerRejectsPasswordOver72Utf8Bytes() {
                String oversizedPassword = "😀".repeat(20);

                RegisterRequest request = new RegisterRequest(
                                "test@omnime.com",
                                oversizedPassword,
                                "Test User");

                when(userRepository.existsByEmail("test@omnime.com"))
                                .thenReturn(false);

                assertThrows(
                                InvalidPasswordException.class,
                                () -> authService.register(request));

                verify(passwordEncoder, never())
                                .encode(oversizedPassword);

                verify(userRepository, never())
                                .saveAndFlush(any(User.class));
        }

        @Test
        void registerHandlesDatabaseDuplicateEmailRace() {
                RegisterRequest request = new RegisterRequest(
                                "test@omnime.com",
                                "Password123",
                                "Test User");

                when(userRepository.existsByEmail("test@omnime.com"))
                                .thenReturn(false);

                when(passwordEncoder.encode("Password123"))
                                .thenReturn("hashed-password");

                when(userRepository.saveAndFlush(any(User.class)))
                                .thenThrow(new DataIntegrityViolationException(
                                                "duplicate email"));

                assertThrows(
                                EmailAlreadyRegisteredException.class,
                                () -> authService.register(request));
        }

        @Test
        void getCurrentUserReturnsAuthenticatedUser() {
                UUID userId = UUID.randomUUID();

                User user = mock(User.class);

                when(userRepository.findById(userId))
                                .thenReturn(Optional.of(user));

                when(user.getId()).thenReturn(userId);
                when(user.getEmail()).thenReturn("test@omnime.com");
                when(user.getDisplayName()).thenReturn("Test User");
                when(user.isOnboardingCompleted()).thenReturn(false);
                when(user.isIncludeAdultAnime()).thenReturn(false);

                AuthUserResponse response = authService.getCurrentUser(userId);

                assertEquals(userId, response.id());
                assertEquals("test@omnime.com", response.email());
                assertEquals("Test User", response.displayName());
                assertFalse(response.onboardingCompleted());
                assertFalse(response.includeAdultAnime());
        }

        @Test
        void getCurrentUserRejectsDeletedUser() {
                UUID userId = UUID.randomUUID();

                when(userRepository.findById(userId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                AuthenticatedUserNotFoundException.class,
                                () -> authService.getCurrentUser(userId));
        }

        @Test
        void forgotPasswordDoesNothingForUnknownEmail() {
                ForgotPasswordRequest request = new ForgotPasswordRequest("missing@omnime.com");

                when(userRepository.findByEmail("missing@omnime.com"))
                                .thenReturn(Optional.empty());

                authService.createPasswordResetToken(request);

                verify(passwordResetTokenRepository, never())
                                .save(any(PasswordResetToken.class));

                verify(passwordResetDeliveryService, never())
                                .sendResetToken(anyString(), anyString());
        }

        @Test
        void forgotPasswordCreatesAndDeliversResetToken() {
                ForgotPasswordRequest request = new ForgotPasswordRequest("TEST@OMNIME.COM");

                User user = mock(User.class);

                when(userRepository.findByEmail("test@omnime.com"))
                                .thenReturn(Optional.of(user));

                when(user.getEmail())
                                .thenReturn("test@omnime.com");

                when(passwordResetTokenService.generateToken())
                                .thenReturn("raw-reset-token");

                when(passwordResetTokenService.hashToken("raw-reset-token"))
                                .thenReturn("hashed-reset-token");

                authService.createPasswordResetToken(request);

                verify(passwordResetTokenRepository)
                                .deleteAllByUser(user);

                verify(passwordResetTokenRepository)
                                .save(any(PasswordResetToken.class));

                verify(passwordResetDeliveryService)
                                .sendResetToken(
                                                "test@omnime.com",
                                                "raw-reset-token");
        }

        @Test
        void resetPasswordRejectsInvalidToken() {
                ResetPasswordRequest request = new ResetPasswordRequest(
                                "bad-token",
                                "NewPassword123");

                when(passwordResetTokenService.hashToken("bad-token"))
                                .thenReturn("bad-token-hash");

                when(passwordResetTokenRepository
                                .findByTokenHash("bad-token-hash"))
                                .thenReturn(Optional.empty());

                assertThrows(
                                InvalidPasswordResetTokenException.class,
                                () -> authService.resetPassword(request));
        }

        @Test
        void resetPasswordChangesPasswordAndInvalidatesTokens() {
                ResetPasswordRequest request = new ResetPasswordRequest(
                                "valid-token",
                                "NewPassword123");

                User user = mock(User.class);
                PasswordResetToken resetToken = mock(PasswordResetToken.class);

                when(passwordResetTokenService.hashToken("valid-token"))
                                .thenReturn("valid-token-hash");

                when(passwordResetTokenRepository
                                .findByTokenHash("valid-token-hash"))
                                .thenReturn(Optional.of(resetToken));

                when(resetToken.getExpiresAt())
                                .thenReturn(Instant.now().plusSeconds(300));

                when(resetToken.getUser())
                                .thenReturn(user);

                when(passwordEncoder.encode("NewPassword123"))
                                .thenReturn("new-password-hash");

                authService.resetPassword(request);

                verify(user)
                                .changePassword("new-password-hash");

                verify(userRepository)
                                .save(user);

                verify(passwordResetTokenRepository)
                                .deleteAllByUser(user);
        }

        @Test
        void resetPasswordRejectsExpiredToken() {
                ResetPasswordRequest request = new ResetPasswordRequest(
                                "expired-token",
                                "NewPassword123");

                PasswordResetToken resetToken = mock(PasswordResetToken.class);

                when(passwordResetTokenService.hashToken("expired-token"))
                                .thenReturn("expired-token-hash");

                when(passwordResetTokenRepository
                                .findByTokenHash("expired-token-hash"))
                                .thenReturn(Optional.of(resetToken));

                when(resetToken.getExpiresAt())
                                .thenReturn(Instant.now().minusSeconds(300));

                assertThrows(
                                InvalidPasswordResetTokenException.class,
                                () -> authService.resetPassword(request));

                verify(passwordEncoder, never())
                                .encode("NewPassword123");
        }

        @Test
        void updateAdultAnimePreferenceUpdatesAuthenticatedUser() {
                UUID userId = UUID.randomUUID();

                User user = new User(
                                "test@example.com",
                                "hashed-password",
                                "Test User");

                when(userRepository.findById(userId))
                                .thenReturn(Optional.of(user));

                UpdateAdultAnimePreferenceRequest request = new UpdateAdultAnimePreferenceRequest(true);

                AuthUserResponse response = authService.updateAdultAnimePreference(userId, request);

                assertTrue(user.isIncludeAdultAnime());
                assertTrue(response.includeAdultAnime());

                verify(userRepository).save(user);
        }

}