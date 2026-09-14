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
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles("test")
class WorkRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkRepository workRepository;

    @Test
    void 所有ユーザーを条件に作品を取得できる() {

        User owner = new User(
            "owner@example.com",
            "Owner123",
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 9, 12, 12, 0)
        );

        userRepository.saveAndFlush(owner);

        Work work = new Work(
            owner,
            "テスト用マフラー",
            null,
            WorkCategory.SCARF,
            WorkStatus.IN_PROGRESS,
            null,
            LocalDateTime.of(2026, 9, 12, 13, 0)
        );

        workRepository.saveAndFlush(work);

        Optional<Work> result =
            workRepository.findByIdAndUser(
                work.getId(),
                owner
            );

        assertThat(result).isPresent();
        assertThat(result.get().getName())
            .isEqualTo("テスト用マフラー");
        assertThat(result.get().getCategory())
            .isEqualTo(WorkCategory.SCARF);
        assertThat(result.get().getStatus())
            .isEqualTo(WorkStatus.IN_PROGRESS);
    }

    @Test
    void 他ユーザーの作品としては取得できない() {

        User owner = new User(
            "owner2@example.com",
            "Owner456",
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 9, 12, 12, 0)
        );

        User otherUser = new User(
            "other@example.com",
            "Other123",
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 9, 12, 12, 0)
        );

        userRepository.saveAndFlush(owner);
        userRepository.saveAndFlush(otherUser);

        Work work = new Work(
            owner,
            "テスト用帽子",
            null,
            WorkCategory.HAT,
            WorkStatus.IN_PROGRESS,
            null,
            LocalDateTime.of(2026, 9, 12, 13, 0)
        );

        workRepository.saveAndFlush(work);

        Optional<Work> result =
            workRepository.findByIdAndUser(
                work.getId(),
                otherUser
            );

        assertThat(result).isEmpty();
    }
}