package com.example.demo.dto;

import java.time.LocalDate;

import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;

public record WorkDetailResult(
        Long id,
        String name,
        WorkCategory category,
        int stitchCount,
        int rowCount,
        String memo,
        WorkStatus status,
        LocalDate completionDate,
        String basicInfoSubmissionToken,
        boolean hasPhoto
) {
}