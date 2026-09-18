package com.example.demo.security;

import java.util.regex.Pattern;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class KnittingUserDetailsService implements UserDetailsService {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("[A-Za-z0-9]{4,20}");

    private final UserRepository userRepository;

    public KnittingUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        String normalizedUsername =
                username == null ? "" : username.strip();

        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new UsernameNotFoundException(
                    "Invalid username or password"
            );
        }

        User user = userRepository
                .findByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Invalid username or password"
                        )
                );

        return new KnittingUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash()
        );
    }
}