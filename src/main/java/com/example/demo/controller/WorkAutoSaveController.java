package com.example.demo.controller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ErrorResponse;
import com.example.demo.form.CompletionDateUpdateForm;
import com.example.demo.form.CounterUpdateForm;
import com.example.demo.form.MemoUpdateForm;
import com.example.demo.form.StatusUpdateForm;
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.WorkAutoSaveService;

@RestController
@RequestMapping("/works")
public class WorkAutoSaveController {

    private final WorkAutoSaveService workAutoSaveService;
    private final MessageSource messageSource;

    public WorkAutoSaveController(
            WorkAutoSaveService workAutoSaveService,
            MessageSource messageSource) {

        this.workAutoSaveService =
                workAutoSaveService;

        this.messageSource =
                messageSource;
    }

    @PostMapping("/{id}/counter")
    public ResponseEntity<?> updateCounter(
            @PathVariable("id")
            Long workId,
            @Valid
            @ModelAttribute("counterUpdateForm")
            CounterUpdateForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal) {

        if (bindingResult.hasErrors()) {
            return validationError(
                    bindingResult
            );
        }

        return ResponseEntity.ok(
                workAutoSaveService.updateCounter(
                        principal.getUserId(),
                        workId,
                        form.getCounterType(),
                        form.getValue(),
                        form.getExpectedValue()
                )
        );
    }

    @PostMapping("/{id}/memo")
    public ResponseEntity<?> updateMemo(
            @PathVariable("id")
            Long workId,
            @Valid
            @ModelAttribute("memoUpdateForm")
            MemoUpdateForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal) {

        if (bindingResult.hasErrors()) {
            return validationError(
                    bindingResult
            );
        }

        workAutoSaveService.updateMemo(
                principal.getUserId(),
                workId,
                form.getMemo(),
                form.getExpectedMemo()
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable("id")
            Long workId,
            @Valid
            @ModelAttribute("statusUpdateForm")
            StatusUpdateForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal) {

        if (bindingResult.hasErrors()) {
            return validationError(
                    bindingResult
            );
        }

        return ResponseEntity.ok(
                workAutoSaveService.updateStatus(
                        principal.getUserId(),
                        workId,
                        form.getStatus(),
                        form.getExpectedStatus(),
                        form.getExpectedCompletionDate()
                )
        );
    }

    @PostMapping("/{id}/completion-date")
    public ResponseEntity<?> updateCompletionDate(
            @PathVariable("id")
            Long workId,
            @Valid
            @ModelAttribute("completionDateUpdateForm")
            CompletionDateUpdateForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal) {

        if (bindingResult.hasErrors()) {
            return validationError(
                    bindingResult
            );
        }

        return ResponseEntity.ok(
                workAutoSaveService
                        .updateCompletionDate(
                                principal.getUserId(),
                                workId,
                                form.getCompletionDate(),
                                form.getExpectedStatus(),
                                form.getExpectedCompletionDate()
                        )
        );
    }

    private ResponseEntity<ErrorResponse> validationError(
            BindingResult bindingResult) {

        Map<String, String> fieldErrors =
                new LinkedHashMap<>();

        bindingResult.getFieldErrors()
                .stream()
                .sorted(
                        Comparator.comparingInt(
                                this::errorPriority
                        )
                )
                .forEach(error ->
                        fieldErrors.putIfAbsent(
                                error.getField(),
                                resolveErrorMessage(
                                        error
                                )
                        )
                );

        return ResponseEntity
                .badRequest()
                .body(
                        new ErrorResponse(
                                "VALIDATION_ERROR",
                                fieldErrors
                        )
                );
    }

    private String resolveErrorMessage(
            FieldError error) {

        return messageSource.getMessage(
                error,
                LocaleContextHolder.getLocale()
        );
    }

    private int errorPriority(
            FieldError error) {

        if (error.isBindingFailure()) {
            return 0;
        }

        String code = error.getCode();

        if ("NotNull".equals(code)
                || "NotBlank".equals(code)
                || "NotEmpty".equals(code)) {
            return 1;
        }

        return 2;
    }
}