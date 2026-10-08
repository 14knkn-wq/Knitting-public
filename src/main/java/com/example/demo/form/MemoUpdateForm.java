package com.example.demo.form;

import jakarta.validation.constraints.NotNull;

import com.example.demo.form.validation.CodePointLength;

public class MemoUpdateForm {

    @NotNull(message = "制作メモを正しく入力してください。")
    @CodePointLength(
            max = 1000,
            message = "制作メモは1000文字以内で入力してください。"
    )
    private String memo;

    @NotNull(message = "保存状態が正しくありません。画面を再読み込みしてください。")
    @CodePointLength(
            max = 1000,
            message = "保存状態が正しくありません。画面を再読み込みしてください。"
    )
    private String expectedMemo;

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getExpectedMemo() {
        return expectedMemo;
    }

    public void setExpectedMemo(String expectedMemo) {
        this.expectedMemo = expectedMemo;
    }
}