package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.example.demo.domain.YarnCombinationMode;
import com.example.demo.dto.YarnCombinationResult;

class YarnCombinationServiceTest {

    private final YarnCombinationService service =
            new YarnCombinationService();

    @Test
    void calculatesDoubleStrand() {

        YarnCombinationResult result =
                service.calculateCombination(
                        YarnCombinationMode.DOUBLE_STRAND,
                        new BigDecimal("50"),
                        new BigDecimal("120"),
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(120, result.lengthPer100g());
        assertEquals("極太", result.thicknessLabel());
        assertEquals(
                "9～11号（4.8～5.4mm）",
                result.knittingNeedleLabel()
        );
        assertEquals(
                "8/0～10/0号（5.0～6.0mm）",
                result.crochetHookLabel()
        );
    }

    @Test
    void calculatesCombination() {

        YarnCombinationResult result =
                service.calculateCombination(
                        YarnCombinationMode.COMBINATION,
                        null,
                        null,
                        new BigDecimal("50"),
                        new BigDecimal("200"),
                        new BigDecimal("50"),
                        new BigDecimal("200")
                );

        assertEquals(200, result.lengthPer100g());
        assertEquals("並太", result.thicknessLabel());
        assertEquals(
                "6～8号（3.9～4.5mm）",
                result.knittingNeedleLabel()
        );
        assertEquals(
                "5/0～7/0号（3.0～4.0mm）",
                result.crochetHookLabel()
        );
    }

    @Test
    void roundsLengthPer100gHalfUpBeforeThicknessClassification() {

        YarnCombinationResult result =
                service.calculateCombination(
                        YarnCombinationMode.DOUBLE_STRAND,
                        new BigDecimal("100"),
                        new BigDecimal("201"),
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(101, result.lengthPer100g());
        assertEquals("極太", result.thicknessLabel());
    }

    @Test
    void ignoresNonSelectedModeValues() {

        YarnCombinationResult result =
                service.calculateCombination(
                        YarnCombinationMode.DOUBLE_STRAND,
                        new BigDecimal("50"),
                        new BigDecimal("120"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

        assertEquals(120, result.lengthPer100g());
    }
}