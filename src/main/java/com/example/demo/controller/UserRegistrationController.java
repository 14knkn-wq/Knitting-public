package com.example.demo.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.exception.RegistrationValidationException;
import com.example.demo.form.UserRegistrationForm;
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.UserRegistrationService;

@Controller
public class UserRegistrationController {
	
	private final UserRegistrationService userRegistrationService;

	public UserRegistrationController(
	        UserRegistrationService userRegistrationService) {
	    this.userRegistrationService = userRegistrationService;
	}
	
	@GetMapping("/register")
	public String showRegistrationForm(
	        @AuthenticationPrincipal KnittingUserPrincipal principal,
	        Model model) {

	    if (principal != null) {
	        return "redirect:/";
	    }

	    model.addAttribute(
	            "userRegistrationForm",
	            new UserRegistrationForm());

	    return "register";
	}
	
	@PostMapping("/register")
	public String registerUser(
	        @Valid
	        @ModelAttribute("userRegistrationForm")
	        UserRegistrationForm form,
	        BindingResult bindingResult,
	        @AuthenticationPrincipal KnittingUserPrincipal principal) {

	    if (principal != null) {
	        return "redirect:/";
	    }

	    if (bindingResult.hasErrors()) {
	        form.setPassword(null);
	        form.setPasswordConfirmation(null);
	        return "register";
	    }

	    try {
	        userRegistrationService.registerUser(
	                form.getEmail(),
	                form.getUsername(),
	                form.getPassword());

	    } catch (RegistrationValidationException e) {

	        switch (e.getReason()) {
	        case USERNAME_DUPLICATE ->
	            bindingResult.rejectValue(
	                    "username",
	                    e.getReason().name(),
	                    "このユーザー名はすでに使用されています。");

	        case EMAIL_DUPLICATE ->
	            bindingResult.rejectValue(
	                    "email",
	                    e.getReason().name(),
	                    "このメールアドレスはすでに登録されています。");

	        case PASSWORD_BLOCKED ->
	            bindingResult.rejectValue(
	                    "password",
	                    e.getReason().name(),
	                    "このパスワードは推測されやすいため使用できません。別のパスワードを設定してください。");
	        }

	        form.setPassword(null);
	        form.setPasswordConfirmation(null);

	        return "register";
	    }

	    return "redirect:/login?registered";
	}

}