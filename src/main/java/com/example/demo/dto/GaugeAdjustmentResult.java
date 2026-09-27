package com.example.demo.dto;

import java.math.BigDecimal;

public record GaugeAdjustmentResult(
        BigDecimal stitchCalculatedValue,
        Integer adjustedStitchCount,
        BigDecimal rowCalculatedValue,
        Integer adjustedRowCount
) {
}