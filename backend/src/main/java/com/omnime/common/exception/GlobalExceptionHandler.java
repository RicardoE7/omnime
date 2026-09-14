package com.omnime.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<Map<String, String>> handleInvalidCredentials(
                        InvalidCredentialsException exception) {
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                                "message", exception.getMessage()));
        }

        @ExceptionHandler(EmailAlreadyRegisteredException.class)
        public ResponseEntity<Map<String, String>> handleEmailAlreadyRegistered(
                        EmailAlreadyRegisteredException exception) {
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                                "message", exception.getMessage()));
        }

        @ExceptionHandler(InvalidPasswordResetTokenException.class)
        public ResponseEntity<Map<String, String>> handleInvalidPasswordResetToken(
                        InvalidPasswordResetTokenException exception) {
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "message", exception.getMessage()));
        }

        @ExceptionHandler(InvalidPasswordException.class)
        public ResponseEntity<Map<String, String>> handleInvalidPassword(
                        InvalidPasswordException exception) {
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "message", exception.getMessage()));
        }

        @ExceptionHandler(AuthenticatedUserNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleAuthenticatedUserNotFound(
                        AuthenticatedUserNotFoundException exception) {

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("message", exception.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidationErrors(
                        MethodArgumentNotValidException exception) {

                String message = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .findFirst()
                                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                .orElse("Invalid request.");

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", message));
        }
}