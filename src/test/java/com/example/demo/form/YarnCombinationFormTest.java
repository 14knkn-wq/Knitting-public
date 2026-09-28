package com.example.demo.form;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

import com.example.demo.domain.YarnCombinationMode;

class YarnCombinationFormTest {

    @Test
    void defaultModeIsDoubleStrand() {

        YarnCombinationForm form = new YarnCombinationForm();

        assertEquals(
                YarnCombinationMode.DOUBLE_STRAND,
                form.getMode()
        );
    }

    @Test
    void validDoubleStrandInputIsValid() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            YarnCombinationForm form =
                    new YarnCombinationForm();

            form.setDoubleStrandWeight(new BigDecimal("50"));
            form.setDoubleStrandLength(new BigDecimal("120"));

            Set<ConstraintViolation<YarnCombinationForm>> violations =
                    validator.validate(form);

            assertTrue(violations.isEmpty());
        }
    }

    @Test
    void missingDoubleStrandInputsHaveFieldErrors() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            YarnCombinationForm form =
                    new YarnCombinationForm();

            Set<ConstraintViolation<YarnCombinationForm>> violations =
                    validator.validate(form);

            assertEquals(2, violations.size());

            assertTrue(hasViolation(
                    violations,
                    "doubleStrandWeight",
                    "重量を入力してください。"
            ));

            assertTrue(hasViolation(
                    violations,
                    "doubleStrandLength",
                    "長さを入力してください。"
            ));
        }
    }

    @Test
    void validCombinationInputIsValid() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            YarnCombinationForm form =
                    new YarnCombinationForm();

            form.setMode(YarnCombinationMode.COMBINATION);
            form.setYarn1Weight(new BigDecimal("50"));
            form.setYarn1Length(new BigDecimal("120"));
            form.setYarn2Weight(new BigDecimal("40"));
            form.setYarn2Length(new BigDecimal("100"));

            Set<ConstraintViolation<YarnCombinationForm>> violations =
                    validator.validate(form);

            assertTrue(violations.isEmpty());
        }
    }

    @Test
    void missingCombinationInputsHaveFieldErrors() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            YarnCombinationForm form =
                    new YarnCombinationForm();

            form.setMode(YarnCombinationMode.COMBINATION);

            Set<ConstraintViolation<YarnCombinationForm>> violations =
                    validator.validate(form);

            assertEquals(4, violations.size());

            assertTrue(hasViolation(
                    violations,
                    "yarn1Weight",
                    "糸1の重量を入力してください。"
            ));

            assertTrue(hasViolation(
                    violations,
                    "yarn1Length",
                    "糸1の長さを入力してください。"
            ));

            assertTrue(hasViolation(
                    violations,
                    "yarn2Weight",
                    "糸2の重量を入力してください。"
            ));

            assertTrue(hasViolation(
                    violations,
                    "yarn2Length",
                    "糸2の長さを入力してください。"
            ));
        }
    }

    @Test
    void nonSelectedCombinationValuesAreIgnoredInDoubleStrandMode() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            YarnCombinationForm form =
                    new YarnCombinationForm();

            form.setDoubleStrandWeight(new BigDecimal("50"));
            form.setDoubleStrandLength(new BigDecimal("120"));

            form.setYarn1Weight(BigDecimal.ZERO);
            form.setYarn1Length(new BigDecimal("100000"));
            form.setYarn2Weight(new BigDecimal("1.234"));
            form.setYarn2Length(BigDecimal.ZERO);

            Set<ConstraintViolation<YarnCombinationForm>> violations =
                    validator.validate(form);

            assertTrue(violations.isEmpty());
        }
    }

    @Test
    void nonSelectedDoubleStrandValuesAreIgnoredInCombinationMode() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            YarnCombinationForm form =
                    new YarnCombinationForm();

            form.setMode(YarnCombinationMode.COMBINATION);

            form.setYarn1Weight(new BigDecimal("50"));
            form.setYarn1Length(new BigDecimal("120"));
            form.setYarn2Weight(new BigDecimal("40"));
            form.setYarn2Length(new BigDecimal("100"));

            form.setDoubleStrandWeight(BigDecimal.ZERO);
            form.setDoubleStrandLength(new BigDecimal("1.234"));

            Set<ConstraintViolation<YarnCombinationForm>> violations =
                    validator.validate(form);

            assertTrue(violations.isEmpty());
        }
    }

    @Test
    void zeroWeightHasError() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            YarnCombinationForm form =
                    new YarnCombinationForm();

            form.setDoubleStrandWeight(BigDecimal.ZERO);
            form.setDoubleStrandLength(new BigDecimal("120"));

            Set<ConstraintViolation<YarnCombinationForm>> violations =
                    validator.validate(form);

            assertEquals(1, violations.size());

            assertTrue(hasViolation(
                    violations,
                    "doubleStrandWeight",
                    "重量は0より大きい値を入力してください。"
            ));
        }
    }

    @Test
    void valueAboveMaximumHasError() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            YarnCombinationForm form =
                    new YarnCombinationForm();

            form.setDoubleStrandWeight(new BigDecimal("100000"));
            form.setDoubleStrandLength(new BigDecimal("120"));

            Set<ConstraintViolation<YarnCombinationForm>> violations =
                    validator.validate(form);

            assertEquals(1, violations.size());

            assertTrue(hasViolation(
                    violations,
                    "doubleStrandWeight",
                    "重量は99999.99以下で入力してください。"
            ));
        }
    }

    @Test
    void tooManyDecimalPlacesHasError() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            YarnCombinationForm form =
                    new YarnCombinationForm();

            form.setDoubleStrandWeight(new BigDecimal("50.123"));
            form.setDoubleStrandLength(new BigDecimal("120"));

            Set<ConstraintViolation<YarnCombinationForm>> violations =
                    validator.validate(form);

            assertEquals(1, violations.size());

            assertTrue(hasViolation(
                    violations,
                    "doubleStrandWeight",
                    "重量は小数第2位までで入力してください。"
            ));
        }
    }

    @Test
    void minimumAndMaximumValuesAreValid() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            YarnCombinationForm form =
                    new YarnCombinationForm();

            form.setDoubleStrandWeight(new BigDecimal("0.01"));
            form.setDoubleStrandLength(new BigDecimal("99999.99"));

            Set<ConstraintViolation<YarnCombinationForm>> violations =
                    validator.validate(form);

            assertTrue(violations.isEmpty());
        }
    }

    private boolean hasViolation(
            Set<ConstraintViolation<YarnCombinationForm>> violations,
            String fieldName,
            String message
    ) {
        return violations.stream()
                .anyMatch(violation ->
                        fieldName.equals(
                                violation.getPropertyPath().toString()
                        )
                        && message.equals(violation.getMessage())
                );
    }
}