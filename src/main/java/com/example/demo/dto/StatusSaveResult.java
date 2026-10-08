package com.example.demo.dto;

import java.time.LocalDate;

import com.example.demo.entity.WorkStatus;

public record StatusSaveResult(
        WorkStatus status,
        LocalDate completionDate
) {
}