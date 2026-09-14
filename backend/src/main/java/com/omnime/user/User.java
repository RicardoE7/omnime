package com.omnime.user;

import org.hibernate.annotations.UuidGenerator;
import jakarta.persistence.Column;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "include_adult_anime", nullable = false)
    private boolean includeAdultAnime = false;

    @Column(name = "taste_evidence_version", nullable = false)
    private long tasteEvidenceVersion = 0;

    @Column(name = "taste_computed_version", nullable = false)
    private long tasteComputedVersion = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public boolean isIncludeAdultAnime() {
        return includeAdultAnime;
    }

    public long getTasteEvidenceVersion() {
        return tasteEvidenceVersion;
    }

    public long getTasteComputedVersion() {
        return tasteComputedVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeAdultAnimePreference(boolean includeAdultAnime) {
        this.includeAdultAnime = includeAdultAnime;
    }
}