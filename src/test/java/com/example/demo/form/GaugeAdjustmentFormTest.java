package com.example.demo.form;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

class GaugeAdjustmentFormTest {

    @Test
    void allFieldsEmptyIsValid() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            GaugeAdjustmentForm form =
                    new GaugeAdjustmentForm();

            Set<ConstraintViolation<GaugeAdjustmentForm>> violations =
                    validator.validate(form);

            assertTrue(violations.isEmpty());
        }
    }
    
    @Test
    void missingOriginalStitchCountHasFieldError() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            GaugeAdjustmentForm form =
                    new GaugeAdjustmentForm();

            form.setSpecifiedStitchGauge(new BigDecimal("20"));
            form.setMeasuredStitchGauge(new BigDecimal("22"));

            Set<ConstraintViolation<GaugeAdjustmentForm>> violations =
                    validator.validate(form);

            assertEquals(1, violations.size());

            ConstraintViolation<GaugeAdjustmentForm> violation =
                    violations.iterator().next();

            assertEquals(
                    "originalStitchCount",
                    violation.getPropertyPath().toString()
            );

            assertEquals(
                    "元の目数を入力してください。",
                    violation.getMessage()
            );
        }
    }
    
    @Test
    void missingOriginalRowCountHasFieldError() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            GaugeAdjustmentForm form =
                    new GaugeAdjustmentForm();

            form.setSpecifiedRowGauge(new BigDecimal("30"));
            form.setMeasuredRowGauge(new BigDecimal("33"));

            Set<ConstraintViolation<GaugeAdjustmentForm>> violations =
                    validator.validate(form);

            assertEquals(1, violations.size());

            ConstraintViolation<GaugeAdjustmentForm> violation =
                    violations.iterator().next();

            assertEquals(
                    "originalRowCount",
                    violation.getPropertyPath().toString()
            );

            assertEquals(
                    "元の段数を入力してください。",
                    violation.getMessage()
            );
        }
    }
    
    @Test
    void allStitchFieldsFilledIsValid() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            GaugeAdjustmentForm form =
                    new GaugeAdjustmentForm();

            form.setSpecifiedStitchGauge(new BigDecimal("20"));
            form.setMeasuredStitchGauge(new BigDecimal("22"));
            form.setOriginalStitchCount(100);

            Set<ConstraintViolation<GaugeAdjustmentForm>> violations =
                    validator.validate(form);

            assertTrue(violations.isEmpty());
        }
    }
    
    @Test
    void allRowFieldsFilledIsValid() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            GaugeAdjustmentForm form =
                    new GaugeAdjustmentForm();

            form.setSpecifiedRowGauge(new BigDecimal("30"));
            form.setMeasuredRowGauge(new BigDecimal("33"));
            form.setOriginalRowCount(100);

            Set<ConstraintViolation<GaugeAdjustmentForm>> violations =
                    validator.validate(form);

            assertTrue(violations.isEmpty());
        }
    }
    
    @Test
    void specifiedStitchGaugeBelowMinimumHasError() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            GaugeAdjustmentForm form =
                    new GaugeAdjustmentForm();

            form.setSpecifiedStitchGauge(new BigDecimal("0.00"));
            form.setMeasuredStitchGauge(new BigDecimal("22"));
            form.setOriginalStitchCount(100);

            Set<ConstraintViolation<GaugeAdjustmentForm>> violations =
                    validator.validate(form);

            assertEquals(1, violations.size());

            ConstraintViolation<GaugeAdjustmentForm> violation =
                    violations.iterator().next();

            assertEquals(
                    "specifiedStitchGauge",
                    violation.getPropertyPath().toString()
            );

            assertEquals(
            		"指定目ゲージは0.01～999.99の範囲で、小数第2位まで入力してください。",
                    violation.getMessage()
            );
        }
    }
    
    @Test
    void specifiedStitchGaugeWithTooManyDecimalPlacesHasError() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            GaugeAdjustmentForm form =
                    new GaugeAdjustmentForm();

            form.setSpecifiedStitchGauge(new BigDecimal("20.123"));
            form.setMeasuredStitchGauge(new BigDecimal("22"));
            form.setOriginalStitchCount(100);

            Set<ConstraintViolation<GaugeAdjustmentForm>> violations =
                    validator.validate(form);

            assertEquals(1, violations.size());

            ConstraintViolation<GaugeAdjustmentForm> violation =
                    violations.iterator().next();

            assertEquals(
                    "specifiedStitchGauge",
                    violation.getPropertyPath().toString()
            );

            assertEquals(
            		"指定目ゲージは0.01～999.99の範囲で、小数第2位まで入力してください。",
                    violation.getMessage()
            );
        }
    }
    
    @Test
    void originalStitchCountBelowMinimumHasError() {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            GaugeAdjustmentForm form =
                    new GaugeAdjustmentForm();

            form.setSpecifiedStitchGauge(new BigDecimal("20"));
            form.setMeasuredStitchGauge(new BigDecimal("22"));
            form.setOriginalStitchCount(0);

            Set<ConstraintViolation<GaugeAdjustmentForm>> violations =
                    validator.validate(form);

            assertEquals(1, violations.size());

            ConstraintViolation<GaugeAdjustmentForm> violation =
                    violations.iterator().next();

            assertEquals(
                    "originalStitchCount",
                    violation.getPropertyPath().toString()
            );

            assertEquals(
                    "元の目数は1～9999の整数で入力してください。",
                    violation.getMessage()
            );
        }
    }
}