package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.domain.CounterType;
import com.example.demo.entity.User;
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.exception.AutoSaveConflictException;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkRepository;

@SpringBootTest
@ActiveProfiles("test")
class WorkAutoSaveServiceS11IT {

    @Autowired
    private WorkAutoSaveService workAutoSaveService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkRepository workRepository;

    @Test
    void 古いexpected値による再更新は競合になりDB値を上書きしない() {

        User user =
                new User(
                        "autosave-s11-it@example.com",
                        "autosaves11it",
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
                        "自動保存競合テスト",
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

        Long userId =
                user.getId();

        Long workId =
                work.getId();

        try {
            workAutoSaveService.updateCounter(
                    userId,
                    workId,
                    CounterType.STITCH,
                    1,
                    0
            );

            Work afterFirstSave =
                    workRepository
                            .findById(workId)
                            .orElseThrow();

            assertEquals(
                    1,
                    afterFirstSave.getStitchCount()
            );

            LocalDateTime updatedAtAfterFirstSave =
                    afterFirstSave.getLastUpdatedAt();

            assertThrows(
                    AutoSaveConflictException.class,
                    () ->
                        workAutoSaveService
                                .updateCounter(
                                        userId,
                                        workId,
                                        CounterType.STITCH,
                                        2,
                                        0
                                )
            );

            Work afterConflict =
                    workRepository
                            .findById(workId)
                            .orElseThrow();

            assertEquals(
                    1,
                    afterConflict.getStitchCount()
            );

            assertEquals(
                    updatedAtAfterFirstSave,
                    afterConflict.getLastUpdatedAt()
            );

        } finally {

            if (workRepository.existsById(
                    workId
            )) {
                workRepository.deleteById(
                        workId
                );
                workRepository.flush();
            }

            if (userRepository.existsById(
                    userId
            )) {
                userRepository.deleteById(
                        userId
                );
                userRepository.flush();
            }
        }
    }
}