package com.example.demo.dto;

import org.springframework.core.io.Resource;

public record WorkImageResult(
        Resource resource,
        String contentType
) {
}