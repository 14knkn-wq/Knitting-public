package com.example.demo.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class YarnThicknessStandardTest {

    @Test
    void boundaryValuesAreClassifiedCorrectly() {

        assertEquals(
                YarnThicknessStandard.GOKUBOSO,
                YarnThicknessStandard.fromLengthPer100g(800)
        );

        assertEquals(
                YarnThicknessStandard.AIBOSO,
                YarnThicknessStandard.fromLengthPer100g(799)
        );

        assertEquals(
                YarnThicknessStandard.AIBOSO,
                YarnThicknessStandard.fromLengthPer100g(500)
        );

        assertEquals(
                YarnThicknessStandard.CHUBOSO,
                YarnThicknessStandard.fromLengthPer100g(499)
        );

        assertEquals(
                YarnThicknessStandard.CHUBOSO,
                YarnThicknessStandard.fromLengthPer100g(350)
        );

        assertEquals(
                YarnThicknessStandard.AIBUTO,
                YarnThicknessStandard.fromLengthPer100g(349)
        );

        assertEquals(
                YarnThicknessStandard.AIBUTO,
                YarnThicknessStandard.fromLengthPer100g(250)
        );

        assertEquals(
                YarnThicknessStandard.NAMIBUTO,
                YarnThicknessStandard.fromLengthPer100g(249)
        );

        assertEquals(
                YarnThicknessStandard.NAMIBUTO,
                YarnThicknessStandard.fromLengthPer100g(175)
        );

        assertEquals(
                YarnThicknessStandard.GOKUBUTO,
                YarnThicknessStandard.fromLengthPer100g(174)
        );

        assertEquals(
                YarnThicknessStandard.GOKUBUTO,
                YarnThicknessStandard.fromLengthPer100g(101)
        );

        assertEquals(
                YarnThicknessStandard.CHOGOKUBUTO,
                YarnThicknessStandard.fromLengthPer100g(100)
        );

        assertEquals(
                YarnThicknessStandard.CHOGOKUBUTO,
                YarnThicknessStandard.fromLengthPer100g(0)
        );
    }

    @Test
    void negativeLengthIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> YarnThicknessStandard.fromLengthPer100g(-1)
        );
    }
}