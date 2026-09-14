package com.omnime.auth;

import java.util.UUID;

public record AuthUserResponse(
                UUID id,
                String email,
                String displayName,
                boolean onboardingCompleted,
                boolean includeAdultAnime) {
}