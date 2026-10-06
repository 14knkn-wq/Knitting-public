package com.example.demo.dto;

import java.time.LocalDate;

public record CompletedWorkSummary(
        Long id,
        String name,
        LocalDate completionDate,
        boolean hasPhoto
) {
}