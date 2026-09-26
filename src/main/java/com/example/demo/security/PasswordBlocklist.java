package com.example.demo.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PasswordBlocklist {

    private static final String BLOCKLIST_RESOURCE =
            "password-blocklist/10k-most-common.txt";

    private Set<String> blockedPasswords;

    public PasswordBlocklist() {
        blockedPasswords = loadBlocklist();
    }

    private Set<String> loadBlocklist() {
        ClassPathResource resource = new ClassPathResource(BLOCKLIST_RESOURCE);
        Set<String> passwords = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        resource.getInputStream(),
                        StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    passwords.add(line.toLowerCase(Locale.ROOT));
                }
            }

            return passwords;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Password Blocklistの読み込みに失敗しました。",
                    e);
        }
    }
    
    public boolean isBlocked(String password) {
        return blockedPasswords.contains(
                password.toLowerCase(Locale.ROOT));
    }
}