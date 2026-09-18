package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WorkNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleWorkNotFound() {
        return "error/404";
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleMethodArgumentTypeMismatch() {
        return "error/400";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpectedException(Exception exception) {

        logger.error(
                "Unexpected error occurred while processing MVC request.",
                exception
        );

        return "error/500";
    }
}