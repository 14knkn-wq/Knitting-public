package com.example.demo.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class KnittingSecurityPropertiesTest {

    @Test
    void shortRememberMeKeyIsRejected() {
        Validator validator =
                Validation.buildDefaultValidatorFactory()
                        .getValidator();

        KnittingSecurityProperties properties =
                new KnittingSecurityProperties(
                        "short-key",
                        false
                );

        Set<ConstraintViolation<KnittingSecurityProperties>> violations =
                validator.validate(properties);

        assertFalse(violations.isEmpty());
    }
    
    @Test
    void sufficientlyLongRememberMeKeyIsAccepted() {
        Validator validator =
                Validation.buildDefaultValidatorFactory()
                        .getValidator();

        KnittingSecurityProperties properties =
                new KnittingSecurityProperties(
                        "12345678901234567890123456789012",
                        false
                );

        Set<ConstraintViolation<KnittingSecurityProperties>> violations =
                validator.validate(properties);

        assertTrue(violations.isEmpty());
    }
}