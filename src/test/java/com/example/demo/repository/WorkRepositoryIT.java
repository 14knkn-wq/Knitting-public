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

    @Test
    void 制作中一覧用検索では本人の制作中作品だけ取得できる() {

        User owner = new User(
            "list-owner@example.com",
            "ListOwner123",
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 10, 1, 10, 0)
        );

        User otherUser = new User(
            "list-other@example.com",
            "ListOther123",
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 10, 1, 10, 0)
        );

        userRepository.saveAndFlush(owner);
        userRepository.saveAndFlush(otherUser);

        Work ownerInProgress = new Work(
            owner,
            "本人の制作中作品",
            null,
            WorkCategory.HAT,
            WorkStatus.IN_PROGRESS,
            null,
            LocalDateTime.of(2026, 10, 1, 11, 0)
        );

        Work ownerCompleted = new Work(
            owner,
            "本人の完成作品",
            null,
            WorkCategory.SCARF,
            WorkStatus.COMPLETED,
            java.time.LocalDate.of(2026, 10, 1),
            LocalDateTime.of(2026, 10, 1, 12, 0)
        );

        Work otherInProgress = new Work(
            otherUser,
            "他ユーザーの制作中作品",
            null,
            WorkCategory.BAG,
            WorkStatus.IN_PROGRESS,
            null,
            LocalDateTime.of(2026, 10, 1, 13, 0)
        );

        workRepository.saveAndFlush(ownerInProgress);
        workRepository.saveAndFlush(ownerCompleted);
        workRepository.saveAndFlush(otherInProgress);

        var result =
            workRepository.findByUserAndStatus(
                owner,
                WorkStatus.IN_PROGRESS,
                org.springframework.data.domain.PageRequest.of(
                    0,
                    10
                )
            );

        assertThat(result.getContent())
            .containsExactly(ownerInProgress);

        assertThat(result.getTotalElements())
        .isEqualTo(1);
}

@Test
void カテゴリー指定検索では指定カテゴリーの制作中作品だけ取得できる() {

    User owner = new User(
        "category-owner@example.com",
        "CategoryOwner123",
        "{bcrypt}dummy",
        LocalDateTime.of(2026, 10, 1, 10, 0)
    );

    User otherUser = new User(
        "category-other@example.com",
        "CategoryOther123",
        "{bcrypt}dummy",
        LocalDateTime.of(2026, 10, 1, 10, 0)
    );

    userRepository.saveAndFlush(owner);
    userRepository.saveAndFlush(otherUser);

    Work target = new Work(
        owner,
        "本人の制作中帽子",
        null,
        WorkCategory.HAT,
        WorkStatus.IN_PROGRESS,
        null,
        LocalDateTime.of(2026, 10, 1, 11, 0)
    );

    Work differentCategory = new Work(
        owner,
        "本人の制作中マフラー",
        null,
        WorkCategory.SCARF,
        WorkStatus.IN_PROGRESS,
        null,
        LocalDateTime.of(2026, 10, 1, 12, 0)
    );

    Work completedHat = new Work(
        owner,
        "本人の完成帽子",
        null,
        WorkCategory.HAT,
        WorkStatus.COMPLETED,
        java.time.LocalDate.of(2026, 10, 1),
        LocalDateTime.of(2026, 10, 1, 13, 0)
    );

    Work otherUserHat = new Work(
        otherUser,
        "他ユーザーの制作中帽子",
        null,
        WorkCategory.HAT,
        WorkStatus.IN_PROGRESS,
        null,
        LocalDateTime.of(2026, 10, 1, 14, 0)
    );

    workRepository.saveAndFlush(target);
    workRepository.saveAndFlush(differentCategory);
    workRepository.saveAndFlush(completedHat);
    workRepository.saveAndFlush(otherUserHat);

    var result =
        workRepository.findByUserAndStatusAndCategory(
            owner,
            WorkStatus.IN_PROGRESS,
            WorkCategory.HAT,
            org.springframework.data.domain.PageRequest.of(
                0,
                10
            )
        );

    assertThat(result.getContent())
        .containsExactly(target);

    assertThat(result.getTotalElements())
        .isEqualTo(1);
}
}
