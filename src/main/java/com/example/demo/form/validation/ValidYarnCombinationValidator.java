package com.example.demo.form.validation;

import java.math.BigDecimal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.example.demo.domain.YarnCombinationMode;
import com.example.demo.form.YarnCombinationForm;

public class ValidYarnCombinationValidator
        implements ConstraintValidator<ValidYarnCombination, YarnCombinationForm> {

    private static final BigDecimal MAX_VALUE =
            new BigDecimal("99999.99");

    @Override
    public boolean isValid(
            YarnCombinationForm form,
            ConstraintValidatorContext context
    ) {
        if (form == null) {
            return true;
        }

        YarnCombinationMode mode = form.getMode();

        if (mode == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        if (mode == YarnCombinationMode.DOUBLE_STRAND) {
            return validateDoubleStrand(form, context);
        }

        if (mode == YarnCombinationMode.COMBINATION) {
            return validateCombination(form, context);
        }

        return true;
    }

    private boolean validateDoubleStrand(
            YarnCombinationForm form,
            ConstraintValidatorContext context
    ) {
        boolean valid = true;

        valid &= validateValue(
                form.getDoubleStrandWeight(),
                context,
                "doubleStrandWeight",
                "重量"
        );

        valid &= validateValue(
                form.getDoubleStrandLength(),
                context,
                "doubleStrandLength",
                "長さ"
        );

        return valid;
    }

    private boolean validateCombination(
            YarnCombinationForm form,
            ConstraintValidatorContext context
    ) {
        boolean valid = true;

        valid &= validateValue(
                form.getYarn1Weight(),
                context,
                "yarn1Weight",
                "糸1の重量"
        );

        valid &= validateValue(
                form.getYarn1Length(),
                context,
                "yarn1Length",
                "糸1の長さ"
        );

        valid &= validateValue(
                form.getYarn2Weight(),
                context,
                "yarn2Weight",
                "糸2の重量"
        );

        valid &= validateValue(
                form.getYarn2Length(),
                context,
                "yarn2Length",
                "糸2の長さ"
        );

        return valid;
    }

    private boolean validateValue(
            BigDecimal value,
            ConstraintValidatorContext context,
            String fieldName,
            String displayName
    ) {
        if (value == null) {
            addFieldError(
                    context,
                    fieldName,
                    displayName + "を入力してください。"
            );
            return false;
        }

        boolean valid = true;

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            addFieldError(
                    context,
                    fieldName,
                    displayName + "は0より大きい値を入力してください。"
            );
            valid = false;
        }

        if (value.compareTo(MAX_VALUE) > 0) {
            addFieldError(
                    context,
                    fieldName,
                    displayName + "は99999.99以下で入力してください。"
            );
            valid = false;
        }

        if (value.scale() > 2) {
            addFieldError(
                    context,
                    fieldName,
                    displayName + "は小数第2位までで入力してください。"
            );
            valid = false;
        }

        return valid;
    }

    private void addFieldError(
            ConstraintValidatorContext context,
            String fieldName,
            String message
    ) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(fieldName)
                .addConstraintViolation();
    }
}