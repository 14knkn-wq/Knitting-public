package com.example.demo.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.example.demo.domain.CounterType;

public class CounterUpdateForm {

    @NotNull(message = "カウンターの種類が正しくありません。")
    private CounterType counterType;

    @NotNull(message = "0～9999の整数で入力してください。")
    @Min(value = 0, message = "0～9999の整数で入力してください。")
    @Max(value = 9999, message = "0～9999の整数で入力してください。")
    private Integer value;

    @NotNull(message = "保存状態が正しくありません。画面を再読み込みしてください。")
    @Min(
            value = 0,
            message = "保存状態が正しくありません。画面を再読み込みしてください。"
    )
    @Max(
            value = 9999,
            message = "保存状態が正しくありません。画面を再読み込みしてください。"
    )
    private Integer expectedValue;

    public CounterType getCounterType() {
        return counterType;
    }

    public void setCounterType(CounterType counterType) {
        this.counterType = counterType;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(Integer expectedValue) {
        this.expectedValue = expectedValue;
    }
}