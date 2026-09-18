package com.example.demo.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class KnittingUserPrincipalTest {

    @Test
    void principalHoldsExpectedValues() {
        KnittingUserPrincipal principal =
                new KnittingUserPrincipal(
                        1L,
                        "Mame123",
                        "{bcrypt}dummyHash"
                );

        assertEquals(1L, principal.getUserId());
        assertEquals("Mame123", principal.getUsername());
        assertEquals("{bcrypt}dummyHash", principal.getPassword());

        assertTrue(principal.getAuthorities().isEmpty());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
    }

    @Test
    void eraseCredentialsRemovesPasswordHash() {
        KnittingUserPrincipal principal =
                new KnittingUserPrincipal(
                        1L,
                        "Mame123",
                        "{bcrypt}dummyHash"
                );

        principal.eraseCredentials();

        assertNull(principal.getPassword());
    }
}