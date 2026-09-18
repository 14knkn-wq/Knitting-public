package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.security.KnittingUserPrincipal;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLogin(
            @AuthenticationPrincipal KnittingUserPrincipal principal) {

        if (principal != null) {
            return "redirect:/";
        }

        return "login";
    }
}