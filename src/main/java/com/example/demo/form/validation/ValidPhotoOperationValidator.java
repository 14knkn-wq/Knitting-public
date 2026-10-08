package com.example.demo.form.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.form.WorkBasicInfoForm;

public class ValidPhotoOperationValidator
        implements ConstraintValidator<ValidPhotoOperation, WorkBasicInfoForm> {

    @Override
    public boolean isValid(
            WorkBasicInfoForm form,
            ConstraintValidatorContext context
    ) {
        if (form == null) {
            return true;
        }

        MultipartFile photo = form.getPhoto();

        boolean hasSelectedPhoto =
                photo != null
                && photo.getOriginalFilename() != null
                && !photo.getOriginalFilename().isEmpty();

        boolean deletePhoto =
                Boolean.TRUE.equals(form.getDeletePhoto());

        if (!hasSelectedPhoto || !deletePhoto) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate(
                "写真の変更と削除を同時に行うことはできません。"
        )
                .addPropertyNode("photo")
                .addConstraintViolation();

        return false;
    }
}