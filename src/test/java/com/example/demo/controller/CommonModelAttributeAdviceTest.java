package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import com.example.demo.security.KnittingUserPrincipal;

class CommonModelAttributeAdviceTest {

    private final CommonModelAttributeAdvice advice =
            new CommonModelAttributeAdvice();

    @Test
    void unauthenticatedUserReturnsFalse() {

        boolean result = advice.isAuthenticated(null);

        assertFalse(result);
    }
    
    @Test
    void authenticatedKnittingUserReturnsTrue() {

        KnittingUserPrincipal principal =
                new KnittingUserPrincipal(
                        1L,
                        "testuser",
                        "password-hash"
                );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        boolean result = advice.isAuthenticated(authentication);

        assertTrue(result);
    }
    
    @Test
    void anonymousAuthenticationReturnsFalse() {

        Authentication authentication =
                new AnonymousAuthenticationToken(
                        "test-key",
                        "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
                );

        boolean result = advice.isAuthenticated(authentication);

        assertFalse(result);
    }
    
    @Test
    void rememberMeAuthenticationReturnsTrue() {

        KnittingUserPrincipal principal =
                new KnittingUserPrincipal(
                        1L,
                        "testuser",
                        "password-hash"
                );

        Authentication authentication =
                new RememberMeAuthenticationToken(
                        "test-key",
                        principal,
                        principal.getAuthorities()
                );

        boolean result = advice.isAuthenticated(authentication);

        assertTrue(result);
    }@Test
    void unauthenticatedKnittingUserReturnsFalse() {

        KnittingUserPrincipal principal =
                new KnittingUserPrincipal(
                        1L,
                        "testuser",
                        "password-hash"
                );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null
                );

        boolean result = advice.isAuthenticated(authentication);

        assertFalse(result);
    }
}