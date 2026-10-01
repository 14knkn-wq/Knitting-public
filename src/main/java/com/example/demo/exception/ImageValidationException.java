package com.example.demo.exception;

public class ImageValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Reason reason;

    public ImageValidationException(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        EMPTY_OR_UNREADABLE,
        FILE_TOO_LARGE,
        UNSUPPORTED_FORMAT,
        DIMENSIONS_TOO_LARGE,
        DECODE_FAILED
    }
}