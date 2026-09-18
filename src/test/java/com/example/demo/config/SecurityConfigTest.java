package com.example.demo.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

	@Test
	void passwordEncoderUsesDelegatingBcryptFormat() {

	    KnittingSecurityProperties securityProperties =
	            new KnittingSecurityProperties(
	                    "12345678901234567890123456789012",
	                    false
	            );

	    UserDetailsService userDetailsService =
	            mock(UserDetailsService.class);

	    SecurityConfig securityConfig =
	            new SecurityConfig(
	                    securityProperties,
	                    userDetailsService
	            );

	    PasswordEncoder passwordEncoder =
	            securityConfig.passwordEncoder();

	    String rawPassword = "TestPassword123!";
	    String encodedPassword =
	            passwordEncoder.encode(rawPassword);

	    assertTrue(encodedPassword.startsWith("{bcrypt}"));
	    assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
	    assertFalse(passwordEncoder.matches(
	            "WrongPassword",
	            encodedPassword
	    ));
	}
}