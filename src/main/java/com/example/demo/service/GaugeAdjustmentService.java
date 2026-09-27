package com.example.demo.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.example.demo.dto.GaugeAdjustmentResult;

@Service
public class GaugeAdjustmentService {

    public GaugeAdjustmentResult calculateAdjustment(
            BigDecimal specifiedStitchGauge,
            BigDecimal measuredStitchGauge,
            Integer originalStitchCount,
            BigDecimal specifiedRowGauge,
            BigDecimal measuredRowGauge,
            Integer originalRowCount
    ) {
        BigDecimal stitchCalculatedValue = null;
        Integer adjustedStitchCount = null;

        if (specifiedStitchGauge != null
                && measuredStitchGauge != null
                && originalStitchCount != null) {

            stitchCalculatedValue =
                    BigDecimal.valueOf(originalStitchCount)
                            .multiply(measuredStitchGauge)
                            .divide(
                                    specifiedStitchGauge,
                                    MathContext.DECIMAL128
                            );

            adjustedStitchCount =
                    stitchCalculatedValue
                            .setScale(0, RoundingMode.HALF_UP)
                            .intValueExact();
        }

        BigDecimal rowCalculatedValue = null;
        Integer adjustedRowCount = null;

        if (specifiedRowGauge != null
                && measuredRowGauge != null
                && originalRowCount != null) {

            rowCalculatedValue =
                    BigDecimal.valueOf(originalRowCount)
                            .multiply(measuredRowGauge)
                            .divide(
                                    specifiedRowGauge,
                                    MathContext.DECIMAL128
                            );

            adjustedRowCount =
                    rowCalculatedValue
                            .setScale(0, RoundingMode.HALF_UP)
                            .intValueExact();
        }

        return new GaugeAdjustmentResult(
                stitchCalculatedValue,
                adjustedStitchCount,
                rowCalculatedValue,
                adjustedRowCount
        );
    }
}