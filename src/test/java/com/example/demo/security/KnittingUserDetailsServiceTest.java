package com.example.demo.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.demo.repository.UserRepository;

class KnittingUserDetailsServiceTest {

    @Test
    void invalidUsernameDoesNotAccessRepository() {
        UserRepository userRepository = mock(UserRepository.class);

        KnittingUserDetailsService userDetailsService =
                new KnittingUserDetailsService(userRepository);

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("ab!")
        );

        verifyNoInteractions(userRepository);
    }
    
    @Test
    void usernameIsStrippedBeforeRepositoryLookup() {
        UserRepository userRepository = mock(UserRepository.class);

        KnittingUserDetailsService userDetailsService =
                new KnittingUserDetailsService(userRepository);

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("  Mame123  ")
        );

        verify(userRepository)
                .findByUsernameIgnoreCase("Mame123");
    }
}