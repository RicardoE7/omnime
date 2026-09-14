package com.omnime.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import com.omnime.user.User;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteAllByUser(User user);
}