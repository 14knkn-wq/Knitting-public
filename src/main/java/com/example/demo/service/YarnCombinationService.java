package com.example.demo.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.example.demo.domain.YarnCombinationMode;
import com.example.demo.domain.YarnThicknessStandard;
import com.example.demo.dto.YarnCombinationResult;

@Service
public class YarnCombinationService {

    public YarnCombinationResult calculateCombination(
            YarnCombinationMode mode,
            BigDecimal doubleStrandWeight,
            BigDecimal doubleStrandLength,
            BigDecimal yarn1Weight,
            BigDecimal yarn1Length,
            BigDecimal yarn2Weight,
            BigDecimal yarn2Length
    ) {
        if (mode == null) {
            throw new IllegalArgumentException(
                    "mode must not be null."
            );
        }

        BigDecimal combinedWeightPerMeter;

        if (mode == YarnCombinationMode.DOUBLE_STRAND) {

            BigDecimal weightPerMeter =
                    doubleStrandWeight.divide(
                            doubleStrandLength,
                            MathContext.DECIMAL128
                    );

            combinedWeightPerMeter =
                    weightPerMeter.multiply(
                            BigDecimal.valueOf(2)
                    );

        } else if (mode == YarnCombinationMode.COMBINATION) {

            BigDecimal yarn1WeightPerMeter =
                    yarn1Weight.divide(
                            yarn1Length,
                            MathContext.DECIMAL128
                    );

            BigDecimal yarn2WeightPerMeter =
                    yarn2Weight.divide(
                            yarn2Length,
                            MathContext.DECIMAL128
                    );

            combinedWeightPerMeter =
                    yarn1WeightPerMeter.add(
                            yarn2WeightPerMeter
                    );

        } else {
            throw new IllegalArgumentException(
                    "Unsupported yarn combination mode."
            );
        }

        BigDecimal calculatedLengthPer100g =
                BigDecimal.valueOf(100)
                        .divide(
                                combinedWeightPerMeter,
                                MathContext.DECIMAL128
                        );

        long lengthPer100g =
                calculatedLengthPer100g
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact();

        YarnThicknessStandard standard =
                YarnThicknessStandard.fromLengthPer100g(
                        lengthPer100g
                );

        String knittingNeedleLabel =
                standard.getKnittingNeedleNumberLabel()
                        + "（"
                        + standard.getKnittingNeedleMmLabel()
                        + "）";

        String crochetHookLabel =
                standard.getCrochetHookNumberLabel()
                        + "（"
                        + standard.getCrochetHookMmLabel()
                        + "）";

        return new YarnCombinationResult(
                lengthPer100g,
                standard.getDisplayName(),
                knittingNeedleLabel,
                crochetHookLabel
        );
    }
}