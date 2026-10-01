package com.example.demo.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.example.demo.exception.InvalidSubmissionTokenException;

@Component
@SessionScope
public class SubmissionTokenManager {

    private final Map<String, TokenEntry> tokens = new HashMap<>();

    public synchronized String issueWorkCreateToken() {
        String token = UUID.randomUUID().toString();

        tokens.put(
                token,
                new TokenEntry(
                        TokenPurpose.WORK_CREATE,
                        TokenState.AVAILABLE
                )
        );

        return token;
    }

    public synchronized void beginWorkCreate(String token) {
        TokenEntry entry = tokens.get(token);

        if (entry == null
                || entry.purpose() != TokenPurpose.WORK_CREATE
                || entry.state() != TokenState.AVAILABLE) {
            throw new InvalidSubmissionTokenException();
        }

        tokens.put(
                token,
                new TokenEntry(
                        entry.purpose(),
                        TokenState.PROCESSING
                )
        );
    }

    public synchronized void complete(String token) {
        if (tokens.remove(token) == null) {
            throw new InvalidSubmissionTokenException();
        }
    }

    public synchronized void releaseForRetry(String token) {
        TokenEntry entry = tokens.get(token);

        if (entry == null
                || entry.state() != TokenState.PROCESSING) {
            throw new InvalidSubmissionTokenException();
        }

        tokens.put(
                token,
                new TokenEntry(
                        entry.purpose(),
                        TokenState.AVAILABLE
                )
        );
    }

    public synchronized void invalidate(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }

    private enum TokenPurpose {
        WORK_CREATE
    }

    private enum TokenState {
        AVAILABLE,
        PROCESSING
    }

    private record TokenEntry(
            TokenPurpose purpose,
            TokenState state
    ) {
    }
}