package com.example.demo.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class KnittingUserPrincipal
        implements UserDetails, CredentialsContainer, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private String passwordHash;

    public KnittingUserPrincipal(
            Long userId,
            String username,
            String passwordHash) {

        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}