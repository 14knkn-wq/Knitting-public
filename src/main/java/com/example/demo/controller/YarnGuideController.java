package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.domain.YarnThicknessStandard;

@Controller
public class YarnGuideController {

    @GetMapping("/yarn-guide")
    public String showYarnGuide(Model model) {

        model.addAttribute(
                "yarnStandards",
                YarnThicknessStandard.values()
        );

        return "yarn-guide";
    }
}