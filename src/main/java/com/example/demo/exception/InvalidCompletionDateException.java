package com.example.demo.exception;

public class InvalidCompletionDateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Reason reason;

    public InvalidCompletionDateException(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        FUTURE_DATE,
        WORK_NOT_COMPLETED
    }
}