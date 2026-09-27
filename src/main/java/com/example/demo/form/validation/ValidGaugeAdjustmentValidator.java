package com.example.demo.form.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.example.demo.form.GaugeAdjustmentForm;

public class ValidGaugeAdjustmentValidator
        implements ConstraintValidator<ValidGaugeAdjustment, GaugeAdjustmentForm> {

    @Override
    public boolean isValid(
            GaugeAdjustmentForm form,
            ConstraintValidatorContext context
    ) {
        if (form == null) {
            return true;
        }

        boolean hasAnyStitchValue =
                form.getSpecifiedStitchGauge() != null
                || form.getMeasuredStitchGauge() != null
                || form.getOriginalStitchCount() != null;

        boolean hasAllStitchValues =
                form.getSpecifiedStitchGauge() != null
                && form.getMeasuredStitchGauge() != null
                && form.getOriginalStitchCount() != null;

        boolean hasAnyRowValue =
                form.getSpecifiedRowGauge() != null
                || form.getMeasuredRowGauge() != null
                || form.getOriginalRowCount() != null;

        boolean hasAllRowValues =
                form.getSpecifiedRowGauge() != null
                && form.getMeasuredRowGauge() != null
                && form.getOriginalRowCount() != null;

        boolean stitchValid =
                !hasAnyStitchValue || hasAllStitchValues;

        boolean rowValid =
                !hasAnyRowValue || hasAllRowValues;

        if (stitchValid && rowValid) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        if (hasAnyStitchValue && !hasAllStitchValues) {

            if (form.getSpecifiedStitchGauge() == null) {
                addFieldError(
                        context,
                        "specifiedStitchGauge",
                        "指定目ゲージを入力してください。"
                );
            }

            if (form.getMeasuredStitchGauge() == null) {
                addFieldError(
                        context,
                        "measuredStitchGauge",
                        "実測目ゲージを入力してください。"
                );
            }

            if (form.getOriginalStitchCount() == null) {
                addFieldError(
                        context,
                        "originalStitchCount",
                        "元の目数を入力してください。"
                );
            }
        }

        if (hasAnyRowValue && !hasAllRowValues) {

            if (form.getSpecifiedRowGauge() == null) {
                addFieldError(
                        context,
                        "specifiedRowGauge",
                        "指定段ゲージを入力してください。"
                );
            }

            if (form.getMeasuredRowGauge() == null) {
                addFieldError(
                        context,
                        "measuredRowGauge",
                        "実測段ゲージを入力してください。"
                );
            }

            if (form.getOriginalRowCount() == null) {
                addFieldError(
                        context,
                        "originalRowCount",
                        "元の段数を入力してください。"
                );
            }
        }

        return false;
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