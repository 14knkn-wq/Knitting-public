package com.example.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.demo.security.KnittingUserPrincipal;

@ControllerAdvice
public class CommonModelAttributeAdvice {

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(Authentication authentication) {

        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal()
                        instanceof KnittingUserPrincipal;
    }
}