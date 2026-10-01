package com.example.demo.form;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;

class WorkCreateFormTest {

    @Test
    void 作品名が空なら必須エラーになる() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName("");
        form.setCategory(
                WorkCategory.HAT
        );
        form.setStatus(
                WorkStatus.IN_PROGRESS
        );
        form.setSubmissionToken(
                "test-token"
        );

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator =
                    factory.getValidator();

            Set<ConstraintViolation<WorkCreateForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(
                            violation ->
                                    violation
                                            .getPropertyPath()
                                            .toString()
                                            .equals("name")
                                    &&
                                    violation
                                            .getMessage()
                                            .equals(
                                                    "作品名を入力してください。"
                                            )
                    )
            );
        }
    }
    
    @Test
    void 作品名は50コードポイントまで許可し51コードポイントでエラーになる() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setCategory(
                WorkCategory.HAT
        );
        form.setStatus(
                WorkStatus.IN_PROGRESS
        );
        form.setSubmissionToken(
                "test-token"
        );

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator =
                    factory.getValidator();

            form.setName(
                    "😀".repeat(50)
            );

            Set<ConstraintViolation<WorkCreateForm>>
                    validViolations =
                            validator.validate(form);

            assertFalse(
                    validViolations.stream()
                            .anyMatch(
                                    violation ->
                                            violation
                                                    .getPropertyPath()
                                                    .toString()
                                                    .equals("name")
                            )
            );

            form.setName(
                    "😀".repeat(51)
            );

            Set<ConstraintViolation<WorkCreateForm>>
                    invalidViolations =
                            validator.validate(form);

            assertTrue(
                    invalidViolations.stream()
                            .anyMatch(
                                    violation ->
                                            violation
                                                    .getPropertyPath()
                                                    .toString()
                                                    .equals("name")
                                            &&
                                            violation
                                                    .getMessage()
                                                    .equals(
                                                            "作品名は50文字以内で入力してください。"
                                                    )
                            )
            );
        }
    }
    
    @Test
    void カテゴリー未選択なら必須エラーになる() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName(
                "テスト作品"
        );
        form.setCategory(null);
        form.setStatus(
                WorkStatus.IN_PROGRESS
        );
        form.setSubmissionToken(
                "test-token"
        );

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator =
                    factory.getValidator();

            Set<ConstraintViolation<WorkCreateForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream()
                            .anyMatch(
                                    violation ->
                                            violation
                                                    .getPropertyPath()
                                                    .toString()
                                                    .equals("category")
                                            &&
                                            violation
                                                    .getMessage()
                                                    .equals(
                                                            "カテゴリーを選択してください。"
                                                    )
                            )
            );
        }
    }
    
    @Test
    void 制作ステータス未選択なら必須エラーになる() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName(
                "テスト作品"
        );
        form.setCategory(
                WorkCategory.HAT
        );
        form.setStatus(null);
        form.setSubmissionToken(
                "test-token"
        );

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator =
                    factory.getValidator();

            Set<ConstraintViolation<WorkCreateForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream()
                            .anyMatch(
                                    violation ->
                                            violation
                                                    .getPropertyPath()
                                                    .toString()
                                                    .equals("status")
                                            &&
                                            violation
                                                    .getMessage()
                                                    .equals(
                                                            "制作ステータスを選択してください。"
                                                    )
                            )
            );
        }
    }
    
    @Test
    void submissionTokenが空なら必須エラーになる() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName(
                "テスト作品"
        );
        form.setCategory(
                WorkCategory.HAT
        );
        form.setStatus(
                WorkStatus.IN_PROGRESS
        );
        form.setSubmissionToken("");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator =
                    factory.getValidator();

            Set<ConstraintViolation<WorkCreateForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream()
                            .anyMatch(
                                    violation ->
                                            violation
                                                    .getPropertyPath()
                                                    .toString()
                                                    .equals(
                                                            "submissionToken"
                                                    )
                            )
            );
        }
    }
    
    @Test
    void 作品名の前後空白は除去される() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName(
                "  テスト作品  "
        );

        assertEquals(
                "テスト作品",
                form.getName()
        );
    }
    
    @Test
    void 正常入力ならValidationエラーはない() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName(
                "テスト作品"
        );
        form.setCategory(
                WorkCategory.HAT
        );
        form.setStatus(
                WorkStatus.IN_PROGRESS
        );
        form.setSubmissionToken(
                "test-token"
        );

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator =
                    factory.getValidator();

            Set<ConstraintViolation<WorkCreateForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.isEmpty()
            );
        }
    }
}