package com.example.demo.config;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("knitting.security")
@Validated
public record KnittingSecurityProperties(

        @NotBlank
        String rememberMeKey,

        @NotNull
        Boolean rememberMeSecureCookie

) {

    @AssertTrue(message = "Remember-Me key must be at least 32 bytes.")
    public boolean isRememberMeKeyMinimumLengthValid() {

        if (rememberMeKey == null || rememberMeKey.isBlank()) {
            return true;
        }

        return rememberMeKey
                .getBytes(StandardCharsets.UTF_8)
                .length >= 32;
    }
}