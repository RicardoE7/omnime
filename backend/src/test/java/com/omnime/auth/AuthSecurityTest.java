package com.omnime.auth;

import com.omnime.common.config.SecurityConfig;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthSecurityTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private AuthCookieService authCookieService;

        @TestConfiguration
        static class TestConfig {

                @Bean
                JwtAuthenticationFilter jwtAuthenticationFilter(
                                JwtService jwtService) {

                        return new JwtAuthenticationFilter(jwtService);
                }
        }

        @Test
        void meRejectsUnauthenticatedRequest() throws Exception {
                mockMvc.perform(get("/api/auth/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void adultAnimePreferenceWithAuthenticationAndCsrfSucceeds() throws Exception {
                UUID userId = UUID.randomUUID();

                AuthUserResponse response = new AuthUserResponse(
                                userId,
                                "test@omnime.com",
                                "Test User",
                                false,
                                true);

                when(authService.updateAdultAnimePreference(
                                org.mockito.ArgumentMatchers.eq(userId),
                                org.mockito.ArgumentMatchers.any(UpdateAdultAnimePreferenceRequest.class)))
                                .thenReturn(response);

                UsernamePasswordAuthenticationToken authenticatedUser = new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.emptyList());

                mockMvc.perform(
                                patch("/api/auth/adult-anime-preference")
                                                .with(authentication(authenticatedUser))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "includeAdultAnime": true
                                                                }
                                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(userId.toString()))
                                .andExpect(jsonPath("$.includeAdultAnime").value(true));
        }

        @Test
        void adultAnimePreferenceWithoutCsrfIsRejected() throws Exception {
                UUID userId = UUID.randomUUID();

                UsernamePasswordAuthenticationToken authenticatedUser = new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.emptyList());

                mockMvc.perform(
                                patch("/api/auth/adult-anime-preference")
                                                .with(authentication(authenticatedUser))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "includeAdultAnime": true
                                                                }
                                                                """))
                                .andExpect(status().is4xxClientError());
        }

        @Test
        void adultAnimePreferenceRejectsUnauthenticatedRequest() throws Exception {
                mockMvc.perform(
                                patch("/api/auth/adult-anime-preference")
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "includeAdultAnime": true
                                                                }
                                                                """))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void meReturnsAuthenticatedUser() throws Exception {
                UUID userId = UUID.randomUUID();

                AuthUserResponse response = new AuthUserResponse(
                                userId,
                                "test@omnime.com",
                                "Test User",
                                false,
                                false);

                when(authService.getCurrentUser(userId))
                                .thenReturn(response);

                UsernamePasswordAuthenticationToken authenticatedUser = new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.emptyList());

                mockMvc.perform(
                                get("/api/auth/me")
                                                .with(authentication(authenticatedUser)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id")
                                                .value(userId.toString()))
                                .andExpect(jsonPath("$.email")
                                                .value("test@omnime.com"))
                                .andExpect(jsonPath("$.displayName")
                                                .value("Test User"))
                                .andExpect(jsonPath("$.onboardingCompleted")
                                                .value(false))
                                .andExpect(jsonPath("$.includeAdultAnime")
                                                .value(false));
        }

        @Test
        @WithMockUser
        void logoutWithoutCsrfIsRejected() throws Exception {
                mockMvc.perform(post("/api/auth/logout"))
                                .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockUser
        void logoutWithCsrfSucceeds() throws Exception {
                when(authCookieService.clearAuthCookie())
                                .thenReturn(
                                                org.springframework.http.ResponseCookie
                                                                .from("omnime_auth", "")
                                                                .httpOnly(true)
                                                                .path("/")
                                                                .maxAge(0)
                                                                .build());

                mockMvc.perform(
                                post("/api/auth/logout")
                                                .with(csrf()))
                                .andExpect(status().isNoContent())
                                .andExpect(cookie().maxAge("omnime_auth", 0));
        }

        @Test
        void registerRejectsInvalidRequest() throws Exception {
                mockMvc.perform(
                                post("/api/auth/register")
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "email": "not-an-email",
                                                                    "password": "short",
                                                                    "displayName": null
                                                                }
                                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void loginRejectsInvalidRequest() throws Exception {
                mockMvc.perform(
                                post("/api/auth/login")
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "email": "not-an-email",
                                                                    "password": ""
                                                                }
                                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void resetPasswordRejectsInvalidRequest() throws Exception {
                mockMvc.perform(
                                post("/api/auth/reset-password")
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "token": "",
                                                                    "newPassword": "short"
                                                                }
                                                                """))
                                .andExpect(status().isBadRequest());
        }
}