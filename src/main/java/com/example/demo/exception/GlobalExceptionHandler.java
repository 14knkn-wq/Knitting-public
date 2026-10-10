package com.example.demo.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    @ExceptionHandler(
            BasicInfoConflictException.class
    )
    public RedirectView handleBasicInfoConflict(
            BasicInfoConflictException exception,
            HttpServletRequest request) {

        RequestContextUtils
                .getOutputFlashMap(request)
                .put(
                        "basicInfoConflictMessage",
                        "別の画面で作品情報が更新されたため、今回の変更は保存されませんでした。"
                        + "最新の内容を確認して、もう一度編集してください。"
                );

        String redirectUrl =
                "/works/" + exception.getWorkId();

        String from = request.getParameter("from");

        if ("in-progress".equals(from)
                || "completed".equals(from)) {
            redirectUrl += "?from=" + from;
        }

        RedirectView redirectView =
                new RedirectView(
                        redirectUrl,
                        true
                );

        redirectView.setStatusCode(
                HttpStatus.SEE_OTHER
        );

        return redirectView;
    }

    @ExceptionHandler(WorkNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleWorkNotFound() {
        return "error/404";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFound() {
        return "error/404";
    }

    @ExceptionHandler(
            MethodArgumentTypeMismatchException.class
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleMethodArgumentTypeMismatch() {
        return "error/400";
    }

    @ExceptionHandler(
            MaxUploadSizeExceededException.class
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleMaxUploadSizeExceeded(
            Model model
    ) {

        model.addAttribute(
                "errorMessage",
                "画像は10MB以下のファイルを選択してください。"
        );

        return "error/400";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(
            HttpStatus.INTERNAL_SERVER_ERROR
    )
    public String handleUnexpectedException(
            Exception exception) {

        logger.error(
                "Unexpected error occurred while processing MVC request.",
                exception
        );

        return "error/500";
    }
}