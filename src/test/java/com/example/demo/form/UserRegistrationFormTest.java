package com.example.demo.form;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

class UserRegistrationFormTest {

    @Test
    void emailは前後空白を除去して小文字化される() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("  Test@Example.COM  ");

        assertEquals("test@example.com", form.getEmail());
    }
    
    @Test
    void usernameは前後空白だけ除去される() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setUsername("  User123  ");

        assertEquals("User123", form.getUsername());
    }
    
    @Test
    void passwordは加工されない() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setPassword("  PassWord123!  ");

        assertEquals("  PassWord123!  ", form.getPassword());
    }
    
    @Test
    void passwordConfirmationは加工されない() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setPasswordConfirmation("  PassWord123!  ");

        assertEquals("  PassWord123!  ", form.getPasswordConfirmation());
    }
    
    @Test
    void passwordが不一致ならpasswordConfirmationにエラーが付く() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("User123");
        form.setPassword("Password123!");
        form.setPasswordConfirmation("Different123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("passwordConfirmation")
                            && violation.getMessage()
                                    .equals("パスワードが一致していません。"))
            );
        }
    }
    
    @Test
    void passwordが一致していれば一致エラーは付かない() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("User123");
        form.setPassword("Password123!");
        form.setPasswordConfirmation("Password123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().noneMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("passwordConfirmation")
                            && violation.getMessage()
                                    .equals("パスワードが一致していません。"))
            );
        }
    }
    
    @Test
    void passwordConfirmationが空なら必須エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("User123");
        form.setPassword("Password123!");
        form.setPasswordConfirmation("");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("passwordConfirmation")
                            && violation.getMessage()
                                    .equals("確認用パスワードを入力してください。"))
            );
        }
    }
    
    @Test
    void passwordが8文字未満なら文字数エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("User123");
        form.setPassword("Pass123");
        form.setPasswordConfirmation("Pass123");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("password")
                            && violation.getMessage()
                                    .equals("パスワードは8～64文字で入力してください。"))
            );
        }
    }
    
    @Test
    void passwordに非ASCII文字が含まれていれば形式エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("User123");
        form.setPassword("Passwordあ123!");
        form.setPasswordConfirmation("Passwordあ123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("password")
                            && violation.getMessage()
                                    .equals("パスワードは半角の英数字・記号・スペースで入力してください。"))
            );
        }
    }
    
    @Test
    void passwordが64文字を超えたら文字数エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        String password = "a".repeat(65);

        form.setEmail("test@example.com");
        form.setUsername("User123");
        form.setPassword(password);
        form.setPasswordConfirmation(password);

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("password")
                            && violation.getMessage()
                                    .equals("パスワードは8～64文字で入力してください。"))
            );
        }
    }
    
    @Test
    void passwordに半角スペースが含まれていても形式エラーにならない() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("User123");
        form.setPassword("Pass word123!");
        form.setPasswordConfirmation("Pass word123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().noneMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("password")
                            && violation.getMessage()
                                    .equals("パスワードは半角の英数字・記号・スペースで入力してください。"))
            );
        }
    }
    
    @Test
    void passwordConfirmationが半角スペースだけでも必須エラーにはならない() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("User123");
        form.setPassword("        ");
        form.setPasswordConfirmation("        ");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().noneMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("passwordConfirmation")
                            && violation.getMessage()
                                    .equals("確認用パスワードを入力してください。"))
            );
        }
    }
    
    @Test
    void emailが空なら必須エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("");
        form.setUsername("User123");
        form.setPassword("Password123!");
        form.setPasswordConfirmation("Password123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("email")
                            && violation.getMessage()
                                    .equals("メールアドレスを入力してください。"))
            );
        }
    }
    
    @Test
    void emailの形式が不正なら形式エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("invalid-email");
        form.setUsername("User123");
        form.setPassword("Password123!");
        form.setPasswordConfirmation("Password123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("email")
                            && violation.getMessage()
                                    .equals("正しいメールアドレス形式で入力してください。"))
            );
        }
    }
    
    @Test
    void usernameが空なら必須エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("");
        form.setPassword("Password123!");
        form.setPasswordConfirmation("Password123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("username")
                            && violation.getMessage()
                                    .equals("ユーザー名を入力してください。"))
            );
        }
    }
    
    @Test
    void usernameに半角英数字以外が含まれていれば形式エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("User_123");
        form.setPassword("Password123!");
        form.setPasswordConfirmation("Password123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("username")
                            && violation.getMessage()
                                    .equals("ユーザー名は4～20文字の半角英数字で入力してください。"))
            );
        }
    }
    
    @Test
    void usernameが4文字未満なら文字数エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("abc");
        form.setPassword("Password123!");
        form.setPasswordConfirmation("Password123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("username")
                            && violation.getMessage()
                                    .equals("ユーザー名は4～20文字の半角英数字で入力してください。"))
            );
        }
    }
    
    @Test
    void usernameが20文字を超えたら文字数エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("test@example.com");
        form.setUsername("a".repeat(21));
        form.setPassword("Password123!");
        form.setPasswordConfirmation("Password123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("username")
                            && violation.getMessage()
                                    .equals("ユーザー名は4～20文字の半角英数字で入力してください。"))
            );
        }
    }
    
    @Test
    void emailが255文字を超えたら文字数エラーになる() {
        UserRegistrationForm form = new UserRegistrationForm();

        form.setEmail("a".repeat(244) + "@example.com");
        form.setUsername("User123");
        form.setPassword("Password123!");
        form.setPasswordConfirmation("Password123!");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UserRegistrationForm>> violations =
                    validator.validate(form);

            assertTrue(
                    violations.stream().anyMatch(violation ->
                            violation.getPropertyPath().toString()
                                    .equals("email")
                            && violation.getMessage()
                                    .equals("メールアドレスは255文字以内で入力してください。"))
            );
        }
    }
}