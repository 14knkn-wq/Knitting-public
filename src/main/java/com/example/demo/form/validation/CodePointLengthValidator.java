package com.example.demo.form.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CodePointLengthValidator
        implements ConstraintValidator<CodePointLength, String> {

    private int max;

    @Override
    public void initialize(CodePointLength annotation) {
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        int length = value.codePointCount(0, value.length());

        return length <= max;
    }
}