package com.example.demo.form;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.demo.entity.WorkStatus;

public class StatusUpdateForm {

    @NotNull(message = "制作ステータスを選択してください。")
    private WorkStatus status;

    @NotNull(message = "保存状態が正しくありません。画面を再読み込みしてください。")
    private WorkStatus expectedStatus;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedCompletionDate;

    public WorkStatus getStatus() {
        return status;
    }

    public void setStatus(WorkStatus status) {
        this.status = status;
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