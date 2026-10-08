package com.example.demo.form;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.demo.entity.WorkStatus;

public class CompletionDateUpdateForm {

    @NotNull(message = "完成日を入力してください。")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate completionDate;

    @NotNull(message = "保存状態が正しくありません。画面を再読み込みしてください。")
    private WorkStatus expectedStatus;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedCompletionDate;

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public WorkStatus getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(WorkStatus expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public LocalDate getExpectedCompletionDate() {
        return expectedCompletionDate;
    }

    public void setExpectedCompletionDate(LocalDate expectedCompletionDate) {
        this.expectedCompletionDate = expectedCompletionDate;
    }
}