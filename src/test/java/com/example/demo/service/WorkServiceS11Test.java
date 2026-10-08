package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.dto.WorkDetailResult;
import com.example.demo.dto.WorkImageResult;
import com.example.demo.entity.User;
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.exception.BasicInfoConflictException;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkRepository;
import com.example.demo.service.SubmissionTokenManager.BasicInfoSnapshot;

@ExtendWith(MockitoExtension.class)
class WorkServiceS11Test {

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

    @Mock
    private TransactionStatus transactionStatus;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(
                    Instant.parse(
                            "2026-10-07T06:00:00Z"
                    ),
                    ZoneId.of("Asia/Tokyo")
            );

    private WorkService workService;

    @BeforeEach
    void setUp() {

        workService =
                new WorkService(
                        userRepository,
                        workRepository,
                        FIXED_CLOCK,
                        workImageStorage,
                        submissionTokenManager,
                        transactionTemplate
                );
    }

    @Test
    void 作品詳細取得時に基本情報submissionTokenを発行する() {

        User user = createUser();

        Work work =
                createWork(
                        user,
                        "詳細作品",
                        null
                );

        when(
                userRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                workRepository.findByIdAndUser(
                        100L,
                        user
                )
        ).thenReturn(
                Optional.of(work)
        );

        when(
                submissionTokenManager
                        .issueBasicInfoToken(
                                100L,
                                "詳細作品",
                                WorkCategory.HAT,
                                null
                        )
        ).thenReturn(
                "detail-token"
        );

        WorkDetailResult result =
                workService.getWorkDetail(
                        1L,
                        100L
                );

        assertEquals(
                100L,
                result.id()
        );

        assertEquals(
                "詳細作品",
                result.name()
        );

        assertEquals(
                WorkCategory.HAT,
                result.category()
        );

        assertEquals(
                "detail-token",
                result.basicInfoSubmissionToken()
        );

        assertFalse(
                result.hasPhoto()
        );

        verify(submissionTokenManager)
                .issueBasicInfoToken(
                        100L,
                        "詳細作品",
                        WorkCategory.HAT,
                        null
                );
    }

    @Test
    void 基本情報を正常更新できる() {

    	enableTransactionExecution();

        User user = createUser();

        Work work =
                createWork(
                        user,
                        "変更前作品",
                        null
                );

        BasicInfoSnapshot snapshot =
                new BasicInfoSnapshot(
                        100L,
                        "変更前作品",
                        WorkCategory.HAT,
                        null
                );

        when(
                submissionTokenManager
                        .beginBasicInfo(
                                "basic-token",
                                100L
                        )
        ).thenReturn(snapshot);

        when(
                userRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                workRepository.findByIdAndUser(
                        100L,
                        user
                )
        ).thenReturn(
                Optional.of(work)
        );

        when(
                workRepository.findByIdAndUserId(
                        100L,
                        1L
                )
        ).thenReturn(
                Optional.of(work)
        );

        workService.updateBasicInfo(
                1L,
                100L,
                "変更後作品",
                null,
                WorkCategory.HAT,
                false,
                "basic-token"
        );

        assertEquals(
                "変更後作品",
                work.getName()
        );

        assertEquals(
                WorkCategory.HAT,
                work.getCategory()
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

        verify(submissionTokenManager)
                .complete(
                        "basic-token"
                );
    }

    @Test
    void 基本情報snapshotと現在値が異なる場合は競合になる() {

    	enableTransactionExecution();

        User user = createUser();

        Work work =
                createWork(
                        user,
                        "別画面で変更済み",
                        null
                );

        BasicInfoSnapshot snapshot =
                new BasicInfoSnapshot(
                        100L,
                        "変更前作品",
                        WorkCategory.HAT,
                        null
                );

        when(
                submissionTokenManager
                        .beginBasicInfo(
                                "old-token",
                                100L
                        )
        ).thenReturn(snapshot);

        when(
                userRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                workRepository.findByIdAndUser(
                        100L,
                        user
                )
        ).thenReturn(
                Optional.of(work)
        );

        assertThrows(
                BasicInfoConflictException.class,
                () ->
                    workService.updateBasicInfo(
                            1L,
                            100L,
                            "今回の変更",
                            null,
                            WorkCategory.HAT,
                            false,
                            "old-token"
                    )
        );

        assertEquals(
                "別画面で変更済み",
                work.getName()
        );

        verify(submissionTokenManager)
                .invalidate(
                        "old-token"
                );

        verify(
                submissionTokenManager,
                never()
        ).complete(
                "old-token"
        );
    }

    @Test
    void 写真未登録作品では初期画像を返す() {

        User user = createUser();

        Work work =
                createWork(
                        user,
                        "写真なし作品",
                        null
                );

        Resource defaultImage =
                new ByteArrayResource(
                        new byte[] {1, 2, 3}
                );

        when(
                userRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                workRepository.findByIdAndUser(
                        100L,
                        user
                )
        ).thenReturn(
                Optional.of(work)
        );

        when(
                workImageStorage
                        .readDefaultImage()
        ).thenReturn(
                defaultImage
        );

        WorkImageResult result =
                workService.getWorkImage(
                        1L,
                        100L
                );

        assertSame(
                defaultImage,
                result.resource()
        );

        assertEquals(
                "image/png",
                result.contentType()
        );

        verify(
                workImageStorage,
                never()
        ).readImage(
                any()
        );
    }

    @Test
    void 登録画像ファイルが存在しない場合は初期画像へフォールバックする() {

        User user = createUser();

        String photoReference =
                "11111111-1111-1111-1111-111111111111.jpg";

        Work work =
                createWork(
                        user,
                        "画像消失作品",
                        photoReference
                );

        Resource defaultImage =
                new ByteArrayResource(
                        new byte[] {4, 5, 6}
                );

        when(
                userRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                workRepository.findByIdAndUser(
                        100L,
                        user
                )
        ).thenReturn(
                Optional.of(work)
        );

        when(
                workImageStorage.readImage(
                        photoReference
                )
        ).thenReturn(null);

        when(
                workImageStorage
                        .readDefaultImage()
        ).thenReturn(
                defaultImage
        );

        WorkImageResult result =
                workService.getWorkImage(
                        1L,
                        100L
                );

        assertSame(
                defaultImage,
                result.resource()
        );

        assertEquals(
                "image/png",
                result.contentType()
        );

        verify(workImageStorage)
                .readImage(
                        photoReference
                );

        verify(workImageStorage)
                .readDefaultImage();
    }

    @Test
    void 登録画像が存在する場合はその画像を返す() {

        User user = createUser();

        String photoReference =
                "22222222-2222-2222-2222-222222222222.png";

        Work work =
                createWork(
                        user,
                        "画像あり作品",
                        photoReference
                );

        Resource image =
                new ByteArrayResource(
                        new byte[] {7, 8, 9}
                );

        when(
                userRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                workRepository.findByIdAndUser(
                        100L,
                        user
                )
        ).thenReturn(
                Optional.of(work)
        );

        when(
                workImageStorage.readImage(
                        photoReference
                )
        ).thenReturn(image);

        when(
                workImageStorage
                        .determineContentType(
                                photoReference
                        )
        ).thenReturn(
                "image/png"
        );

        WorkImageResult result =
                workService.getWorkImage(
                        1L,
                        100L
                );

        assertSame(
                image,
                result.resource()
        );

        assertEquals(
                "image/png",
                result.contentType()
        );
    }

    @Test
    void 作品削除時はDB削除後に登録画像も削除する() {

    	enableTransactionExecution();

        User user = createUser();

        String photoReference =
                "33333333-3333-3333-3333-333333333333.jpg";

        Work work =
                createWork(
                        user,
                        "削除対象作品",
                        photoReference
                );

        when(
                workRepository.findByIdAndUserId(
                        100L,
                        1L
                )
        ).thenReturn(
                Optional.of(work)
        );

        workService.deleteWork(
                1L,
                100L
        );

        verify(workRepository)
                .delete(work);

        verify(workImageStorage)
                .deleteImage(
                        photoReference
                );

        assertTrue(
                work.getPhotoReference()
                        .equals(photoReference)
        );
    }

    private void enableTransactionExecution() {

        when(
                transactionTemplate.execute(
                        any()
                )
        ).thenAnswer(invocation -> {

            TransactionCallback<?> callback =
                    invocation.getArgument(0);

            return callback.doInTransaction(
                    transactionStatus
            );
        });
    }

    private User createUser() {

        User user =
                new User(
                        "detail-service@example.com",
                        "detailservice",
                        "{bcrypt}dummy",
                        LocalDateTime.of(
                                2026,
                                9,
                                1,
                                12,
                                0
                        )
                );

        ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );

        return user;
    }

    private Work createWork(
            User user,
            String name,
            String photoReference) {

        Work work =
                new Work(
                        user,
                        name,
                        photoReference,
                        WorkCategory.HAT,
                        WorkStatus.IN_PROGRESS,
                        null,
                        LocalDateTime.of(
                                2026,
                                10,
                                1,
                                12,
                                0
                        )
                );

        ReflectionTestUtils.setField(
                work,
                "id",
                100L
        );

        return work;
    }
}