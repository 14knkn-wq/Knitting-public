package com.example.demo.dto;

import com.example.demo.domain.CounterType;

public record CounterSaveResult(
        CounterType counterType,
        int value
) {
}