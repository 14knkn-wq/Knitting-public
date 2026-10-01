package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("knitting.image")
public record ImageStorageProperties(
        String storageRoot
) {
}