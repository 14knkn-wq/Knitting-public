package com.example.demo.form;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.demo.form.validation.PasswordMatches;


@PasswordMatches
public class UserRegistrationForm {

	@NotBlank(message = "メールアドレスを入力してください。")
	@Size(max = 255, message = "メールアドレスは255文字以内で入力してください。")
	@Email(message = "正しいメールアドレス形式で入力してください。")
	private String email;
	
	@NotBlank(message = "ユーザー名を入力してください。")
	@Size(
	        min = 4,
	        max = 20,
	        message = "ユーザー名は4～20文字の半角英数字で入力してください。"
	)
	@Pattern(
	        regexp = "^[A-Za-z0-9]+$",
	        message = "ユーザー名は4～20文字の半角英数字で入力してください。"
	)
	private String username;
	
	@NotNull(message = "パスワードは8～64文字で入力してください。")
	@Size(
	        min = 8,
	        max = 64,
	        message = "パスワードは8～64文字で入力してください。"
	)
	@Pattern(
	        regexp = "^[\\x20-\\x7E]+$",
	        message = "パスワードは半角の英数字・記号・スペースで入力してください。"
	)
	private String password;
	
	@NotEmpty(message = "確認用パスワードを入力してください。")
	private String passwordConfirmation;
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null
                ? null
                : email.strip().toLowerCase(Locale.ROOT);
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null
                ? null
                : username.strip();
    }
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }
}