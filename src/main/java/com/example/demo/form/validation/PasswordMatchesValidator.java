package com.example.demo.form.validation;

import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.example.demo.form.UserRegistrationForm;

public class PasswordMatchesValidator
        implements ConstraintValidator<PasswordMatches, UserRegistrationForm> {

    @Override
    public boolean isValid(
            UserRegistrationForm form,
            ConstraintValidatorContext context) {

        if (form == null) {
            return true;
        }

        if (Objects.equals(
                form.getPassword(),
                form.getPasswordConfirmation())) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate(
                context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("passwordConfirmation")
                .addConstraintViolation();

        return false;
    }
}