package com.example.demo.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.User;
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkRepositoryS11IT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkRepository workRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 本人所有作品の更新用取得ではPessimisticWriteロックが適用される() {

        User user =
                new User(
                        "repository-s11-owner@example.com",
                        "repos11owner",
                        "{bcrypt}dummy",
                        LocalDateTime.of(
                                2026,
                                10,
                                7,
                                12,
                                0
                        )
                );

        userRepository.saveAndFlush(
                user
        );

        Work work =
                new Work(
                        user,
                        "Repositoryロックテスト",
                        null,
                        WorkCategory.HAT,
                        WorkStatus.IN_PROGRESS,
                        null,
                        LocalDateTime.of(
                                2026,
                                10,
                                7,
                                12,
                                0
                        )
                );

        workRepository.saveAndFlush(
                work
        );

        Long workId =
                work.getId();

        Long userId =
                user.getId();

        entityManager.clear();

        Optional<Work> result =
                workRepository
                        .findByIdAndUserId(
                                workId,
                                userId
                        );

        assertTrue(
                result.isPresent()
        );

        assertEquals(
                workId,
                result.get().getId()
        );

        assertEquals(
                LockModeType.PESSIMISTIC_WRITE,
                entityManager.getLockMode(
                        result.get()
                )
        );
    }

    @Test
    void 他人のuserIdでは更新用作品を取得できない() {

        User owner =
                new User(
                        "repository-s11-owner2@example.com",
                        "repos11owner2",
                        "{bcrypt}dummy",
                        LocalDateTime.of(
                                2026,
                                10,
                                7,
                                12,
                                0
                        )
                );

        User otherUser =
                new User(
                        "repository-s11-other@example.com",
                        "repos11other",
                        "{bcrypt}dummy",
                        LocalDateTime.of(
                                2026,
                                10,
                                7,
                                12,
                                0
                        )
                );

        userRepository.saveAndFlush(
                owner
        );

        userRepository.saveAndFlush(
                otherUser
        );

        Work work =
                new Work(
                        owner,
                        "他人所有確認作品",
                        null,
                        WorkCategory.HAT,
                        WorkStatus.IN_PROGRESS,
                        null,
                        LocalDateTime.of(
                                2026,
                                10,
                                7,
                                12,
                                0
                        )
                );

        workRepository.saveAndFlush(
                work
        );

        Long workId =
                work.getId();

        Long otherUserId =
                otherUser.getId();

        entityManager.clear();

        Optional<Work> result =
                workRepository
                        .findByIdAndUserId(
                                workId,
                                otherUserId
                        );

        assertTrue(
                result.isEmpty()
        );
    }
}