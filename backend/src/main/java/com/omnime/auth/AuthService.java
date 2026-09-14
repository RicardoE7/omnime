package com.omnime.auth;

import com.omnime.user.User;
import com.omnime.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import com.omnime.common.exception.InvalidCredentialsException;
import com.omnime.common.exception.EmailAlreadyRegisteredException;
import com.omnime.common.exception.InvalidPasswordResetTokenException;
import java.nio.charset.StandardCharsets;
import com.omnime.common.exception.InvalidPasswordException;
import org.springframework.dao.DataIntegrityViolationException;
import com.omnime.common.exception.AuthenticatedUserNotFoundException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetTokenService passwordResetTokenService;
    private final PasswordResetDeliveryService passwordResetDeliveryService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetTokenService passwordResetTokenService,
            PasswordResetDeliveryService passwordResetDeliveryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetTokenService = passwordResetTokenService;
        this.passwordResetDeliveryService = passwordResetDeliveryService;
    }

    public User register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        validatePasswordByteLength(request.password());

        String passwordHash = passwordEncoder.encode(request.password());

        String displayName = request.displayName();

        if (displayName != null) {
            displayName = displayName.trim();

            if (displayName.isEmpty()) {
                displayName = null;
            }
        }

        User user = new User(
                normalizedEmail,
                passwordHash,
                displayName);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    public User login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    public AuthUserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isOnboardingCompleted(),
                user.isIncludeAdultAnime());
    }

    @Transactional
    public AuthUserResponse updateAdultAnimePreference(
            UUID userId,
            UpdateAdultAnimePreferenceRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        user.changeAdultAnimePreference(request.includeAdultAnime());

        userRepository.save(user);

        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isOnboardingCompleted(),
                user.isIncludeAdultAnime());
    }

    @Transactional
    public void createPasswordResetToken(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null) {
            return;
        }

        passwordResetTokenRepository.deleteAllByUser(user);

        String rawToken = passwordResetTokenService.generateToken();

        String tokenHash = passwordResetTokenService.hashToken(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken(
                user,
                tokenHash,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        passwordResetTokenRepository.save(resetToken);

        passwordResetDeliveryService.sendResetToken(
                user.getEmail(),
                rawToken);

    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = passwordResetTokenService.hashToken(request.token());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidPasswordResetTokenException());

        if (!resetToken.getExpiresAt().isAfter(Instant.now())) {
            throw new InvalidPasswordResetTokenException();
        }

        User user = resetToken.getUser();

        validatePasswordByteLength(request.newPassword());

        String newPasswordHash = passwordEncoder.encode(request.newPassword());

        user.changePassword(newPasswordHash);

        userRepository.save(user);

        passwordResetTokenRepository.deleteAllByUser(user);
    }

    private void validatePasswordByteLength(String password) {
        int byteLength = password.getBytes(StandardCharsets.UTF_8).length;

        if (byteLength > 72) {
            throw new InvalidPasswordException(
                    "Password must not exceed 72 bytes.");
        }
    }
}