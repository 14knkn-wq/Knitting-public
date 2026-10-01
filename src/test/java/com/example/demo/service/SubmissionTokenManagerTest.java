package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.exception.InvalidSubmissionTokenException;

class SubmissionTokenManagerTest {

    private SubmissionTokenManager submissionTokenManager;

    @BeforeEach
    void setUp() {
        submissionTokenManager = new SubmissionTokenManager();
    }

    @Test
    void 発行した作品登録tokenは処理開始できる() {
        String token =
                submissionTokenManager.issueWorkCreateToken();

        assertDoesNotThrow(
                () -> submissionTokenManager.beginWorkCreate(token)
        );
    }

    @Test
    void 同じtokenで二重に処理開始すると例外になる() {
        String token =
                submissionTokenManager.issueWorkCreateToken();

        submissionTokenManager.beginWorkCreate(token);

        assertThrows(
                InvalidSubmissionTokenException.class,
                () -> submissionTokenManager.beginWorkCreate(token)
        );
    }

    @Test
    void 再試行可能状態へ戻すと同じtokenで再度処理開始できる() {
        String token =
                submissionTokenManager.issueWorkCreateToken();

        submissionTokenManager.beginWorkCreate(token);
        submissionTokenManager.releaseForRetry(token);

        assertDoesNotThrow(
                () -> submissionTokenManager.beginWorkCreate(token)
        );
    }

    @Test
    void 完了したtokenは再利用できない() {
        String token =
                submissionTokenManager.issueWorkCreateToken();

        submissionTokenManager.beginWorkCreate(token);
        submissionTokenManager.complete(token);

        assertThrows(
                InvalidSubmissionTokenException.class,
                () -> submissionTokenManager.beginWorkCreate(token)
        );
    }

    @Test
    void 存在しないtokenでは処理開始できない() {
        assertThrows(
                InvalidSubmissionTokenException.class,
                () -> submissionTokenManager.beginWorkCreate(
                        "invalid-token"
                )
        );
    }
}