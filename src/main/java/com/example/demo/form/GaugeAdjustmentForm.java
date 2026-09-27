package com.example.demo.form;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.example.demo.form.validation.ValidGaugeAdjustment;

@ValidGaugeAdjustment
public class GaugeAdjustmentForm {

    @DecimalMin(
            value = "0.01",
            message = "指定目ゲージは0.01～999.99の範囲で入力してください。"
    )
    @DecimalMax(
            value = "999.99",
            message = "指定目ゲージは0.01～999.99の範囲で入力してください。"
    )
    @Digits(
            integer = 3,
            fraction = 2,
            message = "指定目ゲージは小数第2位までで入力してください。"
    )
    private BigDecimal specifiedStitchGauge;

    @DecimalMin(
            value = "0.01",
            message = "実測目ゲージは0.01～999.99の範囲で入力してください。"
    )
    @DecimalMax(
            value = "999.99",
            message = "実測目ゲージは0.01～999.99の範囲で入力してください。"
    )
    @Digits(
            integer = 3,
            fraction = 2,
            message = "実測目ゲージは小数第2位までで入力してください。"
    )
    private BigDecimal measuredStitchGauge;

    @Min(
            value = 1,
            message = "元の目数は1～9999の整数で入力してください。"
    )
    @Max(
            value = 9999,
            message = "元の目数は1～9999の整数で入力してください。"
    )
    private Integer originalStitchCount;

    @DecimalMin(
            value = "0.01",
            message = "指定段ゲージは0.01～999.99の範囲で入力してください。"
    )
    @DecimalMax(
            value = "999.99",
            message = "指定段ゲージは0.01～999.99の範囲で入力してください。"
    )
    @Digits(
            integer = 3,
            fraction = 2,
            message = "指定段ゲージは小数第2位までで入力してください。"
    )
    private BigDecimal specifiedRowGauge;

    @DecimalMin(
            value = "0.01",
            message = "実測段ゲージは0.01～999.99の範囲で入力してください。"
    )
    @DecimalMax(
            value = "999.99",
            message = "実測段ゲージは0.01～999.99の範囲で入力してください。"
    )
    @Digits(
            integer = 3,
            fraction = 2,
            message = "実測段ゲージは小数第2位までで入力してください。"
    )
    private BigDecimal measuredRowGauge;

    @Min(
            value = 1,
            message = "元の段数は1～9999の整数で入力してください。"
    )
    @Max(
            value = 9999,
            message = "元の段数は1～9999の整数で入力してください。"
    )
    private Integer originalRowCount;

    public BigDecimal getSpecifiedStitchGauge() {
        return specifiedStitchGauge;
    }

    public void setSpecifiedStitchGauge(BigDecimal specifiedStitchGauge) {
        this.specifiedStitchGauge = specifiedStitchGauge;
    }

    public BigDecimal getMeasuredStitchGauge() {
        return measuredStitchGauge;
    }

    public void setMeasuredStitchGauge(BigDecimal measuredStitchGauge) {
        this.measuredStitchGauge = measuredStitchGauge;
    }

    public Integer getOriginalStitchCount() {
        return originalStitchCount;
    }

    public void setOriginalStitchCount(Integer originalStitchCount) {
        this.originalStitchCount = originalStitchCount;
    }

    public BigDecimal getSpecifiedRowGauge() {
        return specifiedRowGauge;
    }

    public void setSpecifiedRowGauge(BigDecimal specifiedRowGauge) {
        this.specifiedRowGauge = specifiedRowGauge;
    }

    public BigDecimal getMeasuredRowGauge() {
        return measuredRowGauge;
    }

    public void setMeasuredRowGauge(BigDecimal measuredRowGauge) {
        this.measuredRowGauge = measuredRowGauge;
    }

    public Integer getOriginalRowCount() {
        return originalRowCount;
    }

    public void setOriginalRowCount(Integer originalRowCount) {
        this.originalRowCount = originalRowCount;
    }
}