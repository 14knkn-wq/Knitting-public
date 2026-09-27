package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.example.demo.dto.GaugeAdjustmentResult;

class GaugeAdjustmentServiceTest {

    private final GaugeAdjustmentService service =
            new GaugeAdjustmentService();

    @Test
    void calculatesStitchAdjustmentOnly() {

        GaugeAdjustmentResult result =
                service.calculateAdjustment(
                        new BigDecimal("20"),
                        new BigDecimal("22"),
                        100,
                        null,
                        null,
                        null
                );

        assertEquals(
                0,
                new BigDecimal("110")
                        .compareTo(result.stitchCalculatedValue())
        );
        assertEquals(110, result.adjustedStitchCount());

        assertNull(result.rowCalculatedValue());
        assertNull(result.adjustedRowCount());
    }
    
    @Test
    void calculatesRowAdjustmentOnly() {

        GaugeAdjustmentResult result =
                service.calculateAdjustment(
                        null,
                        null,
                        null,
                        new BigDecimal("30"),
                        new BigDecimal("33"),
                        100
                );

        assertNull(result.stitchCalculatedValue());
        assertNull(result.adjustedStitchCount());

        assertEquals(
                0,
                new BigDecimal("110")
                        .compareTo(result.rowCalculatedValue())
        );
        assertEquals(110, result.adjustedRowCount());
    }
    
    @Test
    void roundsAdjustedStitchCountHalfUp() {

        GaugeAdjustmentResult result =
                service.calculateAdjustment(
                        new BigDecimal("20"),
                        new BigDecimal("21"),
                        105,
                        null,
                        null,
                        null
                );

        assertEquals(
                0,
                new BigDecimal("110.25")
                        .compareTo(result.stitchCalculatedValue())
        );
        assertEquals(110, result.adjustedStitchCount());
    }
    
    @Test
    void roundsHalfValueUp() {

        GaugeAdjustmentResult result =
                service.calculateAdjustment(
                        new BigDecimal("20"),
                        new BigDecimal("21.10"),
                        100,
                        null,
                        null,
                        null
                );

        assertEquals(
                0,
                new BigDecimal("105.5")
                        .compareTo(result.stitchCalculatedValue())
        );
        assertEquals(106, result.adjustedStitchCount());
    }
}