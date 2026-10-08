package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.domain.CounterType;
import com.example.demo.dto.CompletionDateSaveResult;
import com.example.demo.dto.CounterSaveResult;
import com.example.demo.dto.StatusSaveResult;
import com.example.demo.entity.User;
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.exception.AutoSaveConflictException;
import com.example.demo.exception.InvalidCompletionDateException;
import com.example.demo.exception.InvalidCompletionDateException.Reason;
import com.example.demo.exception.WorkNotFoundException;
import com.example.demo.repository.WorkRepository;

@ExtendWith(MockitoExtension.class)
class WorkAutoSaveServiceTest {

    @Mock
    private WorkRepository workRepository;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(
                    Instant.parse(
                            "2026-10-07T06:00:00Z"
                    ),
                    ZoneId.of("Asia/Tokyo")
            );

    private static final LocalDateTime INITIAL_UPDATED_AT =
            LocalDateTime.of(
                    2026,
                    10,
                    1,
                    12,
                    0
            );

    private WorkAutoSaveService workAutoSaveService;

    @BeforeEach
    void setUp() {
        workAutoSaveService =
                new WorkAutoSaveService(
                        workRepository,
                        FIXED_CLOCK
                );
    }

    @Test
    void 目数を正常に更新できる() {
        Work work =
                createWork(
                        WorkStatus.IN_PROGRESS,
                        null
                );

        ReflectionTestUtils.setField(
                work,
                "stitchCount",
                10
        );

        when(workRepository.findByIdAndUserId(
                100L,
                1L
        )).thenReturn(Optional.of(work));

        CounterSaveResult result =
                workAutoSaveService.updateCounter(
                        1L,
                        100L,
                        CounterType.STITCH,
                        11,
                        10
                );

        assertEquals(
                CounterType.STITCH,
                result.counterType()
        );

        assertEquals(
                11,
                result.value()
        );

        assertEquals(
                11,
                work.getStitchCount()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        10,
                        7,
                        15,
                        0
                ),
                work.getLastUpdatedAt()
        );

        verify(workRepository)
                .findByIdAndUserId(
                        100L,
                        1L
                );
    }

    @Test
    void カウンター同値再送では最終更新日時を変更しない() {
        Work work =
                createWork(
                        WorkStatus.IN_PROGRESS,
                        null
                );

        ReflectionTestUtils.setField(
                work,
                "stitchCount",
                10
        );

        when(workRepository.findByIdAndUserId(
                100L,
                1L
        )).thenReturn(Optional.of(work));

        CounterSaveResult result =
                workAutoSaveService.updateCounter(
                        1L,
                        100L,
                        CounterType.STITCH,
                        10,
                        10
                );

        assertEquals(
                10,
                result.value()
        );

        assertEquals(
                INITIAL_UPDATED_AT,
                work.getLastUpdatedAt()
        );
    }

    @Test
    void カウンターexpected値が現在値と異なる場合は競合になる() {
        Work work =
                createWork(
                        WorkStatus.IN_PROGRESS,
                        null
                );

        ReflectionTestUtils.setField(
                work,
                "stitchCount",
                12
        );

        when(workRepository.findByIdAndUserId(
                100L,
                1L
        )).thenReturn(Optional.of(work));

        assertThrows(
                AutoSaveConflictException.class,
                () ->
                    workAutoSaveService
                            .updateCounter(
                                    1L,
                                    100L,
                                    CounterType.STITCH,
                                    11,
                                    10
                            )
        );

        assertEquals(
                12,
                work.getStitchCount()
        );

        assertEquals(
                INITIAL_UPDATED_AT,
                work.getLastUpdatedAt()
        );
    }

    @Test
    void 制作メモを正常に更新できる() {
        Work work =
                createWork(
                        WorkStatus.IN_PROGRESS,
                        null
                );

        ReflectionTestUtils.setField(
                work,
                "memo",
                "変更前"
        );

        when(workRepository.findByIdAndUserId(
                100L,
                1L
        )).thenReturn(Optional.of(work));

        workAutoSaveService.updateMemo(
                1L,
                100L,
                "変更後",
                "変更前"
        );

        assertEquals(
                "変更後",
                work.getMemo()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        10,
                        7,
                        15,
                        0
                ),
                work.getLastUpdatedAt()
        );
    }

    @Test
    void 制作中から完成へ変更すると当日が完成日になる() {
        Work work =
                createWork(
                        WorkStatus.IN_PROGRESS,
                        null
                );

        when(workRepository.findByIdAndUserId(
                100L,
                1L
        )).thenReturn(Optional.of(work));

        StatusSaveResult result =
                workAutoSaveService.updateStatus(
                        1L,
                        100L,
                        WorkStatus.COMPLETED,
                        WorkStatus.IN_PROGRESS,
                        null
                );

        assertEquals(
                WorkStatus.COMPLETED,
                result.status()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        10,
                        7
                ),
                result.completionDate()
        );

        assertEquals(
                WorkStatus.COMPLETED,
                work.getStatus()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        10,
                        7
                ),
                work.getCompletionDate()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        10,
                        7,
                        15,
                        0
                ),
                work.getLastUpdatedAt()
        );
    }

    @Test
    void 完成から制作中へ戻すと完成日を解除する() {
        Work work =
                createWork(
                        WorkStatus.COMPLETED,
                        LocalDate.of(
                                2026,
                                10,
                                5
                        )
                );

        when(workRepository.findByIdAndUserId(
                100L,
                1L
        )).thenReturn(Optional.of(work));

        StatusSaveResult result =
                workAutoSaveService.updateStatus(
                        1L,
                        100L,
                        WorkStatus.IN_PROGRESS,
                        WorkStatus.COMPLETED,
                        LocalDate.of(
                                2026,
                                10,
                                5
                        )
                );

        assertEquals(
                WorkStatus.IN_PROGRESS,
                result.status()
        );

        assertNull(
                result.completionDate()
        );

        assertEquals(
                WorkStatus.IN_PROGRESS,
                work.getStatus()
        );

        assertNull(
                work.getCompletionDate()
        );
    }

    @Test
    void 完成日の手動変更では最終更新日時を変更しない() {
        Work work =
                createWork(
                        WorkStatus.COMPLETED,
                        LocalDate.of(
                                2026,
                                10,
                                5
                        )
                );

        when(workRepository.findByIdAndUserId(
                100L,
                1L
        )).thenReturn(Optional.of(work));

        CompletionDateSaveResult result =
                workAutoSaveService
                        .updateCompletionDate(
                                1L,
                                100L,
                                LocalDate.of(
                                        2026,
                                        10,
                                        6
                                ),
                                WorkStatus.COMPLETED,
                                LocalDate.of(
                                        2026,
                                        10,
                                        5
                                )
                        );

        assertEquals(
                LocalDate.of(
                        2026,
                        10,
                        6
                ),
                result.completionDate()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        10,
                        6
                ),
                work.getCompletionDate()
        );

        assertEquals(
                INITIAL_UPDATED_AT,
                work.getLastUpdatedAt()
        );
    }

    @Test
    void 未来の完成日は拒否する() {
        Work work =
                createWork(
                        WorkStatus.COMPLETED,
                        LocalDate.of(
                                2026,
                                10,
                                5
                        )
                );

        when(workRepository.findByIdAndUserId(
                100L,
                1L
        )).thenReturn(Optional.of(work));

        InvalidCompletionDateException exception =
                assertThrows(
                        InvalidCompletionDateException.class,
                        () ->
                            workAutoSaveService
                                    .updateCompletionDate(
                                            1L,
                                            100L,
                                            LocalDate.of(
                                                    2026,
                                                    10,
                                                    8
                                            ),
                                            WorkStatus.COMPLETED,
                                            LocalDate.of(
                                                    2026,
                                                    10,
                                                    5
                                            )
                                    )
                );

        assertEquals(
                Reason.FUTURE_DATE,
                exception.getReason()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        10,
                        5
                ),
                work.getCompletionDate()
        );

        assertEquals(
                INITIAL_UPDATED_AT,
                work.getLastUpdatedAt()
        );
    }

    @Test
    void 制作中作品の完成日は手動変更できない() {
        Work work =
                createWork(
                        WorkStatus.IN_PROGRESS,
                        null
                );

        when(workRepository.findByIdAndUserId(
                100L,
                1L
        )).thenReturn(Optional.of(work));

        InvalidCompletionDateException exception =
                assertThrows(
                        InvalidCompletionDateException.class,
                        () ->
                            workAutoSaveService
                                    .updateCompletionDate(
                                            1L,
                                            100L,
                                            LocalDate.of(
                                                    2026,
                                                    10,
                                                    7
                                            ),
                                            WorkStatus.IN_PROGRESS,
                                            null
                                    )
                );

        assertEquals(
                Reason.WORK_NOT_COMPLETED,
                exception.getReason()
        );

        assertNull(
                work.getCompletionDate()
        );
    }

    @Test
    void 本人所有作品を取得できない場合は404用例外になる() {
        when(workRepository.findByIdAndUserId(
                999L,
                1L
        )).thenReturn(Optional.empty());

        assertThrows(
                WorkNotFoundException.class,
                () ->
                    workAutoSaveService
                            .updateCounter(
                                    1L,
                                    999L,
                                    CounterType.STITCH,
                                    1,
                                    0
                            )
        );

        verify(workRepository)
                .findByIdAndUserId(
                        999L,
                        1L
                );
    }

    private Work createWork(
            WorkStatus status,
            LocalDate completionDate) {

        User user =
                new User(
                        "mame@example.com",
                        "Mame123",
                        "{bcrypt}hashed-password",
                        LocalDateTime.of(
                                2026,
                                9,
                                1,
                                12,
                                0
                        )
                );

        Work work =
                new Work(
                        user,
                        "テスト作品",
                        null,
                        WorkCategory.HAT,
                        status,
                        completionDate,
                        INITIAL_UPDATED_AT
                );

        ReflectionTestUtils.setField(
                work,
                "id",
                100L
        );

        return work;
    }
}