package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.dto.CompletedWorkSummary;
import com.example.demo.dto.InProgressWorkSummary;
import com.example.demo.entity.User;
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkRepository;

@ExtendWith(MockitoExtension.class)
class WorkServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkRepository workRepository;

    @Mock
    private WorkImageStorage workImageStorage;

    @Mock
    private SubmissionTokenManager submissionTokenManager;

    @Mock
    private TransactionTemplate transactionTemplate;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(
                    Instant.parse(
                            "2026-09-29T06:00:00Z"
                    ),
                    ZoneId.of("Asia/Tokyo")
            );

    private WorkService workService;

    @BeforeEach
    void setUp() {
        workService = new WorkService(
                userRepository,
                workRepository,
                FIXED_CLOCK,
                workImageStorage,
                submissionTokenManager,
                transactionTemplate
        );
    }

    @Test
    void 画像なしの制作中作品を正常登録できる() {
        User user = new User(
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

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback =
                            invocation.getArgument(0);

                    return callback.doInTransaction(
                            mock(TransactionStatus.class)
                    );
                });

        Work savedWork = new Work(
                user,
                "テスト作品",
                null,
                WorkCategory.HAT,
                WorkStatus.IN_PROGRESS,
                null,
                LocalDateTime.of(
                        2026,
                        9,
                        29,
                        15,
                        0
                )
        );

        ReflectionTestUtils.setField(
                savedWork,
                "id",
                100L
        );

        when(workRepository.save(any(Work.class)))
                .thenReturn(savedWork);

        Long workId = workService.createWork(
                1L,
                "テスト作品",
                null,
                WorkCategory.HAT,
                WorkStatus.IN_PROGRESS,
                "token-1"
        );

        assertEquals(100L, workId);

        verify(submissionTokenManager)
                .beginWorkCreate("token-1");

        verify(submissionTokenManager)
                .complete("token-1");

        verify(
                workImageStorage,
                never()
        ).saveNewImage(any());

        ArgumentCaptor<Work> workCaptor =
                ArgumentCaptor.forClass(
                        Work.class
                );

        verify(workRepository)
                .save(workCaptor.capture());

        Work work = workCaptor.getValue();

        assertSame(user, work.getUser());
        assertEquals(
                "テスト作品",
                work.getName()
        );
        assertNull(work.getPhotoReference());
        assertEquals(
                WorkCategory.HAT,
                work.getCategory()
        );
        assertEquals(
                WorkStatus.IN_PROGRESS,
                work.getStatus()
        );
        assertEquals(0, work.getStitchCount());
        assertEquals(0, work.getRowCount());
        assertEquals("", work.getMemo());
        assertNull(work.getCompletionDate());
        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        29,
                        15,
                        0
                ),
                work.getLastUpdatedAt()
        );
    }
    
    @Test
    void 完成作品を登録すると登録日が完成日になる() {
        User user = new User(
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

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback =
                            invocation.getArgument(0);

                    return callback.doInTransaction(
                            mock(TransactionStatus.class)
                    );
                });

        Work savedWork = new Work(
                user,
                "完成作品",
                null,
                WorkCategory.SCARF,
                WorkStatus.COMPLETED,
                java.time.LocalDate.of(
                        2026,
                        9,
                        29
                ),
                LocalDateTime.of(
                        2026,
                        9,
                        29,
                        15,
                        0
                )
        );

        ReflectionTestUtils.setField(
                savedWork,
                "id",
                101L
        );

        when(workRepository.save(any(Work.class)))
                .thenReturn(savedWork);

        Long workId = workService.createWork(
                1L,
                "完成作品",
                null,
                WorkCategory.SCARF,
                WorkStatus.COMPLETED,
                "token-2"
        );

        assertEquals(101L, workId);

        ArgumentCaptor<Work> workCaptor =
                ArgumentCaptor.forClass(
                        Work.class
                );

        verify(workRepository)
                .save(workCaptor.capture());

        Work work = workCaptor.getValue();

        assertEquals(
                java.time.LocalDate.of(
                        2026,
                        9,
                        29
                ),
                work.getCompletionDate()
        );

        assertEquals(
                WorkStatus.COMPLETED,
                work.getStatus()
        );
    }
    
    @Test
    void 画像ありの作品では保存した画像参照がWorkに設定される() {
        User user = new User(
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

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback =
                            invocation.getArgument(0);

                    return callback.doInTransaction(
                            mock(TransactionStatus.class)
                    );
                });

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "sample.jpg",
                        "image/jpeg",
                        new byte[] {1, 2, 3}
                );

        when(workImageStorage.saveNewImage(photo))
                .thenReturn(
                        "works/"
                        + "550e8400-e29b-41d4-a716-446655440000.jpg"
                );

        Work savedWork = new Work(
                user,
                "画像あり作品",
                "works/"
                + "550e8400-e29b-41d4-a716-446655440000.jpg",
                WorkCategory.BAG,
                WorkStatus.IN_PROGRESS,
                null,
                LocalDateTime.of(
                        2026,
                        9,
                        29,
                        15,
                        0
                )
        );

        ReflectionTestUtils.setField(
                savedWork,
                "id",
                102L
        );

        when(workRepository.save(any(Work.class)))
                .thenReturn(savedWork);

        Long workId = workService.createWork(
                1L,
                "画像あり作品",
                photo,
                WorkCategory.BAG,
                WorkStatus.IN_PROGRESS,
                "token-3"
        );

        assertEquals(102L, workId);

        verify(workImageStorage)
                .saveNewImage(photo);

        ArgumentCaptor<Work> workCaptor =
                ArgumentCaptor.forClass(
                        Work.class
                );

        verify(workRepository)
                .save(workCaptor.capture());

        Work work = workCaptor.getValue();

        assertEquals(
                "works/"
                + "550e8400-e29b-41d4-a716-446655440000.jpg",
                work.getPhotoReference()
        );

        verify(submissionTokenManager)
                .complete("token-3");

        verify(
                workImageStorage,
                never()
        ).deleteImage(anyString());
    }
    
    @Test
    void DB登録失敗時は候補画像を削除してtokenを再試行可能状態へ戻す() {
        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "sample.jpg",
                        "image/jpeg",
                        new byte[] {1, 2, 3}
                );

        String photoReference =
                "works/"
                + "550e8400-e29b-41d4-a716-446655440000.jpg";

        when(workImageStorage.saveNewImage(photo))
                .thenReturn(photoReference);

        when(transactionTemplate.execute(any()))
                .thenThrow(
                        new IllegalStateException(
                                "DB error"
                        )
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> workService.createWork(
                                1L,
                                "失敗テスト",
                                photo,
                                WorkCategory.HAT,
                                WorkStatus.IN_PROGRESS,
                                "token-4"
                        )
                );

        assertEquals(
                "DB error",
                exception.getMessage()
        );

        verify(workImageStorage)
                .deleteImage(photoReference);

        verify(submissionTokenManager)
                .releaseForRetry("token-4");

        verify(
                submissionTokenManager,
                never()
        ).complete("token-4");
    }
    
    @Test
    void 画像Validation失敗時はDB登録せずtokenを再試行可能状態へ戻す() {
        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "broken.jpg",
                        "image/jpeg",
                        new byte[] {1, 2, 3}
                );

        when(workImageStorage.saveNewImage(photo))
                .thenThrow(
                        new com.example.demo.exception.ImageValidationException(
                                com.example.demo.exception.ImageValidationException.Reason.DECODE_FAILED
                        )
                );

        com.example.demo.exception.ImageValidationException exception =
                assertThrows(
                        com.example.demo.exception.ImageValidationException.class,
                        () -> workService.createWork(
                                1L,
                                "画像エラー作品",
                                photo,
                                WorkCategory.HAT,
                                WorkStatus.IN_PROGRESS,
                                "token-5"
                        )
                );

        assertEquals(
                com.example.demo.exception.ImageValidationException.Reason.DECODE_FAILED,
                exception.getReason()
        );

        verify(
                transactionTemplate,
                never()
        ).execute(any());

        verify(
                workImageStorage,
                never()
        ).deleteImage(anyString());

        verify(submissionTokenManager)
                .releaseForRetry("token-5");

        verify(
                submissionTokenManager,
                never()
        ).complete("token-5");
    }
    
    @Test
    void 不正なsubmissionTokenでは画像保存もDB登録も行わない() {
        doThrow(
                new com.example.demo.exception.InvalidSubmissionTokenException()
        ).when(submissionTokenManager)
                .beginWorkCreate("invalid-token");

        assertThrows(
                com.example.demo.exception.InvalidSubmissionTokenException.class,
                () -> workService.createWork(
                        1L,
                        "tokenエラー作品",
                        null,
                        WorkCategory.HAT,
                        WorkStatus.IN_PROGRESS,
                        "invalid-token"
                )
        );

        verify(
                workImageStorage,
                never()
        ).saveNewImage(any());

        verify(
                transactionTemplate,
                never()
        ).execute(any());

        verify(
                submissionTokenManager,
                never()
        ).complete(anyString());

        verify(
                submissionTokenManager,
                never()
        ).releaseForRetry(anyString());
    }
    
    @Test
    void DBcommit後にtoken完了処理が失敗しても保存済み画像は削除しない() {

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "sample.jpg",
                        "image/jpeg",
                        new byte[] {1, 2, 3}
                );

        String photoReference =
                "works/"
                + "550e8400-e29b-41d4-a716-446655440000.jpg";

        when(workImageStorage.saveNewImage(photo))
                .thenReturn(photoReference);

        when(transactionTemplate.execute(any()))
                .thenReturn(103L);

        doThrow(
                new com.example.demo.exception.InvalidSubmissionTokenException()
        ).when(submissionTokenManager)
                .complete("token-6");

        assertThrows(
                com.example.demo.exception.InvalidSubmissionTokenException.class,
                () -> workService.createWork(
                        1L,
                        "commit後テスト",
                        photo,
                        WorkCategory.HAT,
                        WorkStatus.IN_PROGRESS,
                        "token-6"
                )
        );

        verify(
                workImageStorage,
                never()
        ).deleteImage(photoReference);

        verify(
                submissionTokenManager,
                never()
        ).releaseForRetry("token-6");
    }
    
    @Test
    void ゼロbyte画像を選択した場合は画像Validationエラーとして扱う() {

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "empty.jpg",
                        "image/jpeg",
                        new byte[0]
                );

        when(workImageStorage.saveNewImage(photo))
                .thenThrow(
                        new com.example.demo.exception.ImageValidationException(
                                com.example.demo.exception.ImageValidationException.Reason.EMPTY_OR_UNREADABLE
                        )
                );

        com.example.demo.exception.ImageValidationException exception =
                assertThrows(
                        com.example.demo.exception.ImageValidationException.class,
                        () -> workService.createWork(
                                1L,
                                "0byte画像テスト",
                                photo,
                                WorkCategory.HAT,
                                WorkStatus.IN_PROGRESS,
                                "token-zero-byte"
                        )
                );

        assertEquals(
                com.example.demo.exception.ImageValidationException.Reason.EMPTY_OR_UNREADABLE,
                exception.getReason()
        );

        verify(workImageStorage)
                .saveNewImage(photo);

        verify(
                transactionTemplate,
                never()
        ).execute(any());

        verify(submissionTokenManager)
                .releaseForRetry(
                        "token-zero-byte"
                );

        verify(
                submissionTokenManager,
                never()
        ).complete(
                "token-zero-byte"
        );
    }

    @Test
    void 制作中一覧取得では指定ページと並び順を使用してDTOへ変換する() {

        User user = new User(
                "list@example.com",
                "ListUser123",
                "{bcrypt}hashed-password",
                LocalDateTime.of(
                        2026,
                        9,
                        1,
                        12,
                        0
                )
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Work noPhotoWork = new Work(
                user,
                "写真なし作品",
                null,
                WorkCategory.HAT,
                WorkStatus.IN_PROGRESS,
                null,
                LocalDateTime.of(
                        2026,
                        9,
                        29,
                        14,
                        0
                )
        );

        ReflectionTestUtils.setField(
                noPhotoWork,
                "id",
                101L
        );

        ReflectionTestUtils.setField(
                noPhotoWork,
                "stitchCount",
                25
        );

        ReflectionTestUtils.setField(
                noPhotoWork,
                "rowCount",
                40
        );

        Work photoWork = new Work(
                user,
                "写真あり作品",
                "works/test-photo.jpg",
                WorkCategory.BAG,
                WorkStatus.IN_PROGRESS,
                null,
                LocalDateTime.of(
                        2026,
                        9,
                        29,
                        15,
                        0
                )
        );

        ReflectionTestUtils.setField(
                photoWork,
                "id",
                102L
        );

        ReflectionTestUtils.setField(
                photoWork,
                "stitchCount",
                50
        );

        ReflectionTestUtils.setField(
                photoWork,
                "rowCount",
                60
        );

        when(
                workRepository.findByUserAndStatus(
                        eq(user),
                        eq(WorkStatus.IN_PROGRESS),
                        any(Pageable.class)
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(
                                photoWork,
                                noPhotoWork
                        )
                )
        );

        Page<InProgressWorkSummary> result =
                workService.getInProgressWorks(
                        1L,
                        null,
                        2,
                        10
                );

        assertEquals(2, result.getContent().size());

        InProgressWorkSummary first =
                result.getContent().get(0);

        assertEquals(102L, first.id());
        assertEquals(
                "写真あり作品",
                first.name()
        );
        assertEquals(50, first.stitchCount());
        assertEquals(60, first.rowCount());
        assertTrue(first.hasPhoto());

        InProgressWorkSummary second =
                result.getContent().get(1);

        assertEquals(101L, second.id());
        assertEquals(
                "写真なし作品",
                second.name()
        );
        assertEquals(25, second.stitchCount());
        assertEquals(40, second.rowCount());
        assertFalse(second.hasPhoto());

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        verify(workRepository)
                .findByUserAndStatus(
                        eq(user),
                        eq(WorkStatus.IN_PROGRESS),
                        pageableCaptor.capture()
                );

        Pageable pageable =
                pageableCaptor.getValue();

        assertEquals(
                2,
                pageable.getPageNumber()
        );

        assertEquals(
                10,
                pageable.getPageSize()
        );

        Sort.Order lastUpdatedOrder =
                pageable.getSort()
                        .getOrderFor(
                                "lastUpdatedAt"
                        );

        assertNotNull(lastUpdatedOrder);
        assertTrue(
                lastUpdatedOrder.isDescending()
        );

        Sort.Order idOrder =
                pageable.getSort()
                        .getOrderFor("id");

        assertNotNull(idOrder);
        assertTrue(idOrder.isDescending());

        verify(
                workRepository,
                never()
        ).findByUserAndStatusAndCategory(
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void カテゴリー指定時はカテゴリー付きRepository検索を使用する() {

        User user = new User(
                "category-service@example.com",
                "CategoryService123",
                "{bcrypt}hashed-password",
                LocalDateTime.of(
                        2026,
                        9,
                        1,
                        12,
                        0
                )
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Work work = new Work(
                user,
                "帽子作品",
                null,
                WorkCategory.HAT,
                WorkStatus.IN_PROGRESS,
                null,
                LocalDateTime.of(
                        2026,
                        9,
                        29,
                        15,
                        0
                )
        );

        ReflectionTestUtils.setField(
                work,
                "id",
                201L
        );

        when(
                workRepository
                        .findByUserAndStatusAndCategory(
                                eq(user),
                                eq(
                                        WorkStatus.IN_PROGRESS
                                ),
                                eq(WorkCategory.HAT),
                                any(Pageable.class)
                        )
        ).thenReturn(
                new PageImpl<>(
                        List.of(work)
                )
        );

        Page<InProgressWorkSummary> result =
                workService.getInProgressWorks(
                        1L,
                        WorkCategory.HAT,
                        0,
                        10
                );

        assertEquals(
                1,
                result.getContent().size()
        );

        assertEquals(
                201L,
                result.getContent()
                        .get(0)
                        .id()
        );

        verify(workRepository)
                .findByUserAndStatusAndCategory(
                        eq(user),
                        eq(WorkStatus.IN_PROGRESS),
                        eq(WorkCategory.HAT),
                        any(Pageable.class)
                );

        verify(
                workRepository,
                never()
        ).findByUserAndStatus(
                any(),
                any(),
                any()
        );
    }

    @Test
    void 制作中一覧取得時に認証ユーザーが存在しなければ例外となる() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> workService
                                .getInProgressWorks(
                                        999L,
                                        null,
                                        0,
                                        10
                                )
                );

        assertEquals(
                "認証ユーザーが見つかりません。",
                exception.getMessage()
        );

        verifyNoInteractions(workRepository);
    }

    @Test
    void 完成一覧取得では完成作品を10件固定かつ完成日とidの降順で取得してDTOへ変換する() {

        User user = new User(
                "completed-list@example.com",
                "CompletedList123",
                "{bcrypt}hashed-password",
                LocalDateTime.of(
                        2026,
                        9,
                        1,
                        12,
                        0
                )
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Work noPhotoWork = new Work(
                user,
                "写真なし完成作品",
                null,
                WorkCategory.HAT,
                WorkStatus.COMPLETED,
                LocalDate.of(
                        2026,
                        9,
                        28
                ),
                LocalDateTime.of(
                        2026,
                        9,
                        29,
                        14,
                        0
                )
        );

        ReflectionTestUtils.setField(
                noPhotoWork,
                "id",
                301L
        );

        Work photoWork = new Work(
                user,
                "写真あり完成作品",
                "works/completed-photo.jpg",
                WorkCategory.BAG,
                WorkStatus.COMPLETED,
                LocalDate.of(
                        2026,
                        9,
                        29
                ),
                LocalDateTime.of(
                        2026,
                        9,
                        29,
                        15,
                        0
                )
        );

        ReflectionTestUtils.setField(
                photoWork,
                "id",
                302L
        );

        when(
                workRepository.findByUserAndStatus(
                        eq(user),
                        eq(WorkStatus.COMPLETED),
                        any(Pageable.class)
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(
                                photoWork,
                                noPhotoWork
                        )
                )
        );

        Page<CompletedWorkSummary> result =
                workService.getCompletedWorks(
                        1L,
                        null,
                        2
                );

        assertEquals(
                2,
                result.getContent().size()
        );

        CompletedWorkSummary first =
                result.getContent().get(0);

        assertEquals(302L, first.id());
        assertEquals(
                "写真あり完成作品",
                first.name()
        );
        assertEquals(
                LocalDate.of(
                        2026,
                        9,
                        29
                ),
                first.completionDate()
        );
        assertTrue(first.hasPhoto());

        CompletedWorkSummary second =
                result.getContent().get(1);

        assertEquals(301L, second.id());
        assertEquals(
                "写真なし完成作品",
                second.name()
        );
        assertEquals(
                LocalDate.of(
                        2026,
                        9,
                        28
                ),
                second.completionDate()
        );
        assertFalse(second.hasPhoto());

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        verify(workRepository)
                .findByUserAndStatus(
                        eq(user),
                        eq(WorkStatus.COMPLETED),
                        pageableCaptor.capture()
                );

        Pageable pageable =
                pageableCaptor.getValue();

        assertEquals(
                2,
                pageable.getPageNumber()
        );

        assertEquals(
                10,
                pageable.getPageSize()
        );

        Sort.Order completionDateOrder =
                pageable.getSort()
                        .getOrderFor(
                                "completionDate"
                        );

        assertNotNull(completionDateOrder);
        assertTrue(
                completionDateOrder.isDescending()
        );

        Sort.Order idOrder =
                pageable.getSort()
                        .getOrderFor("id");

        assertNotNull(idOrder);
        assertTrue(idOrder.isDescending());

        verify(
                workRepository,
                never()
        ).findByUserAndStatusAndCategory(
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void 完成一覧のカテゴリー指定時はカテゴリー付きRepository検索を使用する() {

        User user = new User(
                "completed-category@example.com",
                "CompletedCategory123",
                "{bcrypt}hashed-password",
                LocalDateTime.of(
                        2026,
                        9,
                        1,
                        12,
                        0
                )
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Work work = new Work(
                user,
                "完成した帽子",
                null,
                WorkCategory.HAT,
                WorkStatus.COMPLETED,
                LocalDate.of(
                        2026,
                        9,
                        29
                ),
                LocalDateTime.of(
                        2026,
                        9,
                        29,
                        15,
                        0
                )
        );

        ReflectionTestUtils.setField(
                work,
                "id",
                401L
        );

        when(
                workRepository
                        .findByUserAndStatusAndCategory(
                                eq(user),
                                eq(
                                        WorkStatus.COMPLETED
                                ),
                                eq(WorkCategory.HAT),
                                any(Pageable.class)
                        )
        ).thenReturn(
                new PageImpl<>(
                        List.of(work)
                )
        );

        Page<CompletedWorkSummary> result =
                workService.getCompletedWorks(
                        1L,
                        WorkCategory.HAT,
                        0
                );

        assertEquals(
                1,
                result.getContent().size()
        );

        assertEquals(
                401L,
                result.getContent()
                        .get(0)
                        .id()
        );

        verify(workRepository)
                .findByUserAndStatusAndCategory(
                        eq(user),
                        eq(WorkStatus.COMPLETED),
                        eq(WorkCategory.HAT),
                        any(Pageable.class)
                );

        verify(
                workRepository,
                never()
        ).findByUserAndStatus(
                any(),
                any(),
                any()
        );
    }

    @Test
    void 完成一覧取得時に認証ユーザーが存在しなければ例外となる() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> workService
                                .getCompletedWorks(
                                        999L,
                                        null,
                                        0
                                )
                );

        assertEquals(
                "認証ユーザーが見つかりません。",
                exception.getMessage()
        );

        verifyNoInteractions(workRepository);
    }
}