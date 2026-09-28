package com.example.demo.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.dto.YarnCombinationResult;
import com.example.demo.form.YarnCombinationForm;
import com.example.demo.service.YarnCombinationService;

@Controller
public class YarnCombinationController {

    private final YarnCombinationService yarnCombinationService;

    public YarnCombinationController(
            YarnCombinationService yarnCombinationService
    ) {
        this.yarnCombinationService = yarnCombinationService;
    }

    @GetMapping("/yarn-combination")
    public String showYarnCombination(
            @RequestParam(
                    name = "calculate",
                    defaultValue = "false"
            )
            boolean calculate,
            @Valid
            @ModelAttribute("yarnCombinationForm")
            YarnCombinationForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (!calculate) {
            return "yarn-combination";
        }

        if (bindingResult.hasErrors()) {
            return "yarn-combination";
        }

        YarnCombinationResult combinationResult =
                yarnCombinationService.calculateCombination(
                        form.getMode(),
                        form.getDoubleStrandWeight(),
                        form.getDoubleStrandLength(),
                        form.getYarn1Weight(),
                        form.getYarn1Length(),
                        form.getYarn2Weight(),
                        form.getYarn2Length()
                );

        model.addAttribute(
                "combinationResult",
                combinationResult
        );

        return "yarn-combination";
    }
}