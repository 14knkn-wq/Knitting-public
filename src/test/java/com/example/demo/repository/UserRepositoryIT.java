package com.example.demo.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.entity.User;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles("test")
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Test
    void usernameを大文字小文字を区別せず検索できる() {

        User user = new User(
            "mame@example.com",
            "Mame123",
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 9, 12, 12, 0)
        );

        userRepository.saveAndFlush(user);

        Optional<User> result =
            userRepository.findByUsernameIgnoreCase("mame123");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("Mame123");
        assertThat(result.get().getEmail()).isEqualTo("mame@example.com");
    }
}