package com.example.demo.exception;

public class RegistrationValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Reason reason;

    public RegistrationValidationException(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        USERNAME_DUPLICATE,
        EMAIL_DUPLICATE,
        PASSWORD_BLOCKED
    }
}