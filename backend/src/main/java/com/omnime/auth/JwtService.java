package com.omnime.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.omnime.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import java.util.UUID;

@Service
public class JwtService {

        private final JwtEncoder jwtEncoder;
        private final JwtDecoder jwtDecoder;
        private final long expirationMinutes;

        public JwtService(
                        @Value("${app.jwt.secret}") String secret,
                        @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {

                byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);

                if (secretBytes.length < 32) {
                        throw new IllegalStateException(
                                        "JWT secret must be at least 32 bytes.");
                }

                SecretKey secretKey = new SecretKeySpec(
                                secretBytes,
                                "HmacSHA256");

                NimbusJwtDecoder decoder = NimbusJwtDecoder
                                .withSecretKey(secretKey)
                                .macAlgorithm(MacAlgorithm.HS256)
                                .build();

                decoder.setJwtValidator(JwtValidators.createDefault());

                this.jwtDecoder = decoder;

                this.jwtEncoder = NimbusJwtEncoder
                                .withSecretKey(secretKey)
                                .algorithm(MacAlgorithm.HS256)
                                .build();

                this.expirationMinutes = expirationMinutes;
        }

        public String generateToken(User user) {
                Instant now = Instant.now();
                Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

                JwtClaimsSet claims = JwtClaimsSet.builder()
                                .subject(user.getId().toString())
                                .issuedAt(now)
                                .expiresAt(expiresAt)
                                .build();

                JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

                return jwtEncoder.encode(
                                JwtEncoderParameters.from(header, claims)).getTokenValue();
        }

        public UUID getUserId(String token) {
                Jwt jwt = jwtDecoder.decode(token);

                return UUID.fromString(jwt.getSubject());
        }
}