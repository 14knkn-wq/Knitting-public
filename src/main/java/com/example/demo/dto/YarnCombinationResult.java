package com.example.demo.dto;

public record YarnCombinationResult(
        long lengthPer100g,
        String thicknessLabel,
        String knittingNeedleLabel,
        String crochetHookLabel
) {
}