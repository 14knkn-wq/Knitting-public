package com.example.demo.form;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import com.example.demo.domain.YarnCombinationMode;
import com.example.demo.form.validation.ValidYarnCombination;

@ValidYarnCombination
public class YarnCombinationForm {

    @NotNull(message = "計算モードを選択してください。")
    private YarnCombinationMode mode = YarnCombinationMode.DOUBLE_STRAND;

    private BigDecimal doubleStrandWeight;

    private BigDecimal doubleStrandLength;

    private BigDecimal yarn1Weight;

    private BigDecimal yarn1Length;

    private BigDecimal yarn2Weight;

    private BigDecimal yarn2Length;

    public YarnCombinationMode getMode() {
        return mode;
    }

    public void setMode(YarnCombinationMode mode) {
        this.mode = mode;
    }

    public BigDecimal getDoubleStrandWeight() {
        return doubleStrandWeight;
    }

    public void setDoubleStrandWeight(BigDecimal doubleStrandWeight) {
        this.doubleStrandWeight = doubleStrandWeight;
    }

    public BigDecimal getDoubleStrandLength() {
        return doubleStrandLength;
    }

    public void setDoubleStrandLength(BigDecimal doubleStrandLength) {
        this.doubleStrandLength = doubleStrandLength;
    }

    public BigDecimal getYarn1Weight() {
        return yarn1Weight;
    }

    public void setYarn1Weight(BigDecimal yarn1Weight) {
        this.yarn1Weight = yarn1Weight;
    }

    public BigDecimal getYarn1Length() {
        return yarn1Length;
    }

    public void setYarn1Length(BigDecimal yarn1Length) {
        this.yarn1Length = yarn1Length;
    }

    public BigDecimal getYarn2Weight() {
        return yarn2Weight;
    }

    public void setYarn2Weight(BigDecimal yarn2Weight) {
        this.yarn2Weight = yarn2Weight;
    }

    public BigDecimal getYarn2Length() {
        return yarn2Length;
    }

    public void setYarn2Length(BigDecimal yarn2Length) {
        this.yarn2Length = yarn2Length;
    }
}