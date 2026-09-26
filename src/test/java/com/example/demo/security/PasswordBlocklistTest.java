package com.example.demo.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordBlocklistTest {

    @Test
    void blocklistに含まれるpasswordは拒否対象になる() {
        PasswordBlocklist passwordBlocklist = new PasswordBlocklist();

        assertTrue(passwordBlocklist.isBlocked("password"));
    }
    
    @Test
    void blocklist照合は大文字小文字を区別しない() {
        PasswordBlocklist passwordBlocklist = new PasswordBlocklist();

        assertTrue(passwordBlocklist.isBlocked("PASSWORD"));
    }
    
    @Test
    void blocklistは部分一致では拒否しない() {
        PasswordBlocklist passwordBlocklist = new PasswordBlocklist();

        assertFalse(passwordBlocklist.isBlocked("my-password"));
    }
    
    @Test
    void blocklist照合では前後空白を除去しない() {
        PasswordBlocklist passwordBlocklist = new PasswordBlocklist();

        assertFalse(passwordBlocklist.isBlocked(" password "));
    }
    
    @Test
    void blocklistの空行は拒否対象として読み込まない() {
        PasswordBlocklist passwordBlocklist = new PasswordBlocklist();

        assertFalse(passwordBlocklist.isBlocked(""));
    }
    
    @Test
    void blocklistに含まれないpasswordは拒否対象にならない() {
        PasswordBlocklist passwordBlocklist = new PasswordBlocklist();

        assertFalse(passwordBlocklist.isBlocked("Knitting-2026!"));
    }
}