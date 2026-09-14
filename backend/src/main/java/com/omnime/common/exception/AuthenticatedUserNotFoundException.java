package com.omnime.common.exception;

public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException() {
        super("Authenticated user no longer exists.");
    }
}