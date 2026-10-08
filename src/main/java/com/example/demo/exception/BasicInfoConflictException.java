package com.example.demo.exception;

public class BasicInfoConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Long workId;

    public BasicInfoConflictException(Long workId) {
        super();
        this.workId = workId;
    }

    public Long getWorkId() {
        return workId;
    }
}