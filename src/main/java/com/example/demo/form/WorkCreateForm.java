package com.example.demo.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.form.validation.CodePointLength;

public class WorkCreateForm {

    @NotBlank(message = "作品名を入力してください。")
    @CodePointLength(
            max = 50,
            message = "作品名は50文字以内で入力してください。"
    )
    private String name;

    private MultipartFile photo;

    @NotNull(message = "カテゴリーを選択してください。")
    private WorkCategory category;

    @NotNull(message = "制作ステータスを選択してください。")
    private WorkStatus status;

    @NotBlank
    private String submissionToken;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null
                ? null
                : name.strip();
    }

    public MultipartFile getPhoto() {
        return photo;
    }

    public void setPhoto(MultipartFile photo) {
        this.photo = photo;
    }

    public WorkCategory getCategory() {
        return category;
    }

    public void setCategory(WorkCategory category) {
        this.category = category;
    }

    public WorkStatus getStatus() {
        return status;
    }

    public void setStatus(WorkStatus status) {
        this.status = status;
    }

    public String getSubmissionToken() {
        return submissionToken;
    }

    public void setSubmissionToken(String submissionToken) {
        this.submissionToken = submissionToken;
    }
}