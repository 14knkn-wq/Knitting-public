package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.WorkService;

@Controller
public class HomeController {

    private final WorkService workService;

    public HomeController(
            WorkService workService
    ) {
        this.workService = workService;
    }

    @GetMapping("/")
    public String showHome(
            @AuthenticationPrincipal KnittingUserPrincipal principal,
            Model model
    ) {
        if (principal != null) {
            model.addAttribute(
                    "inProgressWorks",
                    workService.getInProgressWorks(
                            principal.getUserId(),
                            null,
                            0,
                            4
                    )
            );
        }

        return "home";
    }
}