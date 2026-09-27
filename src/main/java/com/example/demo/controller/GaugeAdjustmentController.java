package com.example.demo.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.demo.dto.GaugeAdjustmentResult;
import com.example.demo.form.GaugeAdjustmentForm;
import com.example.demo.service.GaugeAdjustmentService;

@Controller
public class GaugeAdjustmentController {

    private final GaugeAdjustmentService gaugeAdjustmentService;

    public GaugeAdjustmentController(
            GaugeAdjustmentService gaugeAdjustmentService
    ) {
        this.gaugeAdjustmentService = gaugeAdjustmentService;
    }

    @GetMapping("/gauge-adjustment")
    public String showGaugeAdjustment(
            @Valid
            @ModelAttribute("gaugeAdjustmentForm")
            GaugeAdjustmentForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "gauge-adjustment";
        }

        if (isInitialDisplay(form)) {
            return "gauge-adjustment";
        }

        GaugeAdjustmentResult adjustmentResult =
                gaugeAdjustmentService.calculateAdjustment(
                        form.getSpecifiedStitchGauge(),
                        form.getMeasuredStitchGauge(),
                        form.getOriginalStitchCount(),
                        form.getSpecifiedRowGauge(),
                        form.getMeasuredRowGauge(),
                        form.getOriginalRowCount()
                );

        model.addAttribute(
                "adjustmentResult",
                adjustmentResult
        );

        return "gauge-adjustment";
    }

    private boolean isInitialDisplay(
            GaugeAdjustmentForm form
    ) {
        return form.getSpecifiedStitchGauge() == null
                && form.getMeasuredStitchGauge() == null
                && form.getOriginalStitchCount() == null
                && form.getSpecifiedRowGauge() == null
                && form.getMeasuredRowGauge() == null
                && form.getOriginalRowCount() == null;
    }
}