package com.example.demo.dto;

public record InProgressWorkSummary(
        Long id,
        String name,
        int stitchCount,
        int rowCount,
        boolean hasPhoto
) {
}