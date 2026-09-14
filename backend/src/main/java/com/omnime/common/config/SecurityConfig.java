package com.omnime.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.omnime.auth.JwtAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {
        @Value("${app.frontend-origin}")
        private String frontendOrigin;

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
                CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

                http
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(csrfTokenRepository)
                                                .csrfTokenRequestHandler(csrfHandler))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/health").permitAll()
                                                .requestMatchers(
                                                                "/api/auth/csrf",
                                                                "/api/auth/register",
                                                                "/api/auth/login",
                                                                "/api/auth/forgot-password",
                                                                "/api/auth/reset-password")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(List.of(frontendOrigin));
                configuration.setAllowedMethods(
                                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration("/api/**", configuration);

                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CookieCsrfTokenRepository csrfTokenRepository() {
                return CookieCsrfTokenRepository.withHttpOnlyFalse();
        }
}
