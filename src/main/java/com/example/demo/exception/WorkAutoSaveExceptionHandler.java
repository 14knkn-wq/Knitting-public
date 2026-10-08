package com.example.demo.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.controller.WorkAutoSaveController;
import com.example.demo.dto.ErrorResponse;

@RestControllerAdvice(
        assignableTypes = WorkAutoSaveController.class
)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WorkAutoSaveExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    WorkAutoSaveExceptionHandler.class
            );

    @ExceptionHandler(
            AutoSaveConflictException.class
    )
    public ResponseEntity<ErrorResponse>
            handleAutoSaveConflict() {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ErrorResponse(
                                "CONFLICT",
                                Map.of()
                        )
                );
    }

    @ExceptionHandler(
            InvalidCompletionDateException.class
    )
    public ResponseEntity<ErrorResponse>
            handleInvalidCompletionDate(
                    InvalidCompletionDateException exception) {

        String message =
                switch (exception.getReason()) {
                    case FUTURE_DATE ->
                        "未来の日付は指定できません。";

                    case WORK_NOT_COMPLETED ->
                        "制作中の作品には完成日を設定できません。";
                };

        return ResponseEntity
                .badRequest()
                .body(
                        new ErrorResponse(
                                "VALIDATION_ERROR",
                                Map.of(
                                        "completionDate",
                                        message
                                )
                        )
                );
    }

    @ExceptionHandler(
            WorkNotFoundException.class
    )
    public ResponseEntity<Void>
            handleWorkNotFound() {

        return ResponseEntity
                .notFound()
                .build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void>
            handleUnexpectedException(
                    Exception exception) {

        logger.error(
                "Unexpected error occurred while processing work auto-save request.",
                exception
        );

        return ResponseEntity
                .internalServerError()
                .build();
    }
}