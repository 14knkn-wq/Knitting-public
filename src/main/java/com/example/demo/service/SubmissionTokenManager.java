package com.example.demo.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.example.demo.entity.WorkCategory;
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
                        TokenState.AVAILABLE,
                        null
                )
        );

        return token;
    }

    public synchronized String issueBasicInfoToken(
            Long workId,
            String name,
            WorkCategory category,
            String photoReference) {

        String token = UUID.randomUUID().toString();

        BasicInfoSnapshot snapshot =
                new BasicInfoSnapshot(
                        workId,
                        name,
                        category,
                        photoReference
                );

        tokens.put(
                token,
                new TokenEntry(
                        TokenPurpose.WORK_BASIC_INFO,
                        TokenState.AVAILABLE,
                        snapshot
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
                        TokenState.PROCESSING,
                        entry.basicInfoSnapshot()
                )
        );
    }

    public synchronized BasicInfoSnapshot beginBasicInfo(
            String token,
            Long workId) {

        TokenEntry entry = tokens.get(token);

        if (entry == null
                || entry.purpose() != TokenPurpose.WORK_BASIC_INFO
                || entry.state() != TokenState.AVAILABLE
                || entry.basicInfoSnapshot() == null
                || !entry.basicInfoSnapshot()
                        .workId()
                        .equals(workId)) {
            throw new InvalidSubmissionTokenException();
        }

        tokens.put(
                token,
                new TokenEntry(
                        entry.purpose(),
                        TokenState.PROCESSING,
                        entry.basicInfoSnapshot()
                )
        );

        return entry.basicInfoSnapshot();
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
                        TokenState.AVAILABLE,
                        entry.basicInfoSnapshot()
                )
        );
    }

    public synchronized void invalidate(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }

    public record BasicInfoSnapshot(
            Long workId,
            String name,
            WorkCategory category,
            String photoReference
    ) {
    }

    private enum TokenPurpose {
        WORK_CREATE,
        WORK_BASIC_INFO
    }

    private enum TokenState {
        AVAILABLE,
        PROCESSING
    }

    private record TokenEntry(
            TokenPurpose purpose,
            TokenState state,
            BasicInfoSnapshot basicInfoSnapshot
    ) {
    }
}