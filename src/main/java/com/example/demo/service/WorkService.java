package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.CompletedWorkSummary;
import com.example.demo.dto.InProgressWorkSummary;
import com.example.demo.dto.WorkDetailResult;
import com.example.demo.dto.WorkImageResult;
import com.example.demo.entity.User;
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.exception.BasicInfoConflictException;
import com.example.demo.exception.WorkNotFoundException;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkRepository;
import com.example.demo.service.SubmissionTokenManager.BasicInfoSnapshot;

@Service
public class WorkService {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    WorkService.class
            );

    private final UserRepository userRepository;
    private final WorkRepository workRepository;
    private final Clock clock;
    private final WorkImageStorage workImageStorage;
    private final SubmissionTokenManager submissionTokenManager;
    private final TransactionTemplate transactionTemplate;

    public WorkService(
            UserRepository userRepository,
            WorkRepository workRepository,
            Clock clock,
            WorkImageStorage workImageStorage,
            SubmissionTokenManager submissionTokenManager,
            TransactionTemplate transactionTemplate) {

        this.userRepository = userRepository;
        this.workRepository = workRepository;
        this.clock = clock;
        this.workImageStorage = workImageStorage;
        this.submissionTokenManager =
                submissionTokenManager;
        this.transactionTemplate =
                transactionTemplate;
    }

    private String saveCandidateImage(
            MultipartFile photo) {

        if (photo == null
                || photo.getOriginalFilename() == null
                || photo.getOriginalFilename().isBlank()) {
            return null;
        }

        return workImageStorage.saveNewImage(photo);
    }

    private User getAuthenticatedUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "認証ユーザーが見つかりません。"
                        )
                );
    }

    private Work getOwnedWork(
            Long userId,
            Long workId) {

        User user = getAuthenticatedUser(userId);

        return workRepository
                .findByIdAndUser(
                        workId,
                        user
                )
                .orElseThrow(
                        WorkNotFoundException::new
                );
    }

    private Work getOwnedWorkForUpdate(
            Long userId,
            Long workId) {

        return workRepository
                .findByIdAndUserId(
                        workId,
                        userId
                )
                .orElseThrow(
                        WorkNotFoundException::new
                );
    }

    private LocalDate determineCompletionDate(
            WorkStatus status) {

        if (status == WorkStatus.COMPLETED) {
            return LocalDate.now(clock);
        }

        return null;
    }

    private Work createWorkEntity(
            User user,
            String name,
            String photoReference,
            WorkCategory category,
            WorkStatus status) {

        return new Work(
                user,
                name,
                photoReference,
                category,
                status,
                determineCompletionDate(status),
                LocalDateTime.now(clock)
        );
    }

    private Long saveWorkInTransaction(
            Long userId,
            String name,
            String photoReference,
            WorkCategory category,
            WorkStatus status) {

        Long workId = transactionTemplate.execute(
                transactionStatus -> {

                    User user =
                            getAuthenticatedUser(userId);

                    Work work = createWorkEntity(
                            user,
                            name,
                            photoReference,
                            category,
                            status
                    );

                    Work savedWork =
                            workRepository.save(work);

                    return savedWork.getId();
                }
        );

        if (workId == null) {
            throw new IllegalStateException(
                    "作品登録結果を取得できませんでした。"
            );
        }

        return workId;
    }

    private InProgressWorkSummary toInProgressWorkSummary(
            Work work) {

        return new InProgressWorkSummary(
                work.getId(),
                work.getName(),
                work.getStitchCount(),
                work.getRowCount(),
                work.getPhotoReference() != null
        );
    }

    private CompletedWorkSummary toCompletedWorkSummary(
            Work work) {

        return new CompletedWorkSummary(
                work.getId(),
                work.getName(),
                work.getCompletionDate(),
                work.getPhotoReference() != null
        );
    }

    private WorkDetailResult toWorkDetailResult(
            Work work,
            String submissionToken) {

        return new WorkDetailResult(
                work.getId(),
                work.getName(),
                work.getCategory(),
                work.getStitchCount(),
                work.getRowCount(),
                work.getMemo(),
                work.getStatus(),
                work.getCompletionDate(),
                submissionToken,
                work.getPhotoReference() != null
        );
    }

    private void verifyBasicInfoSnapshot(
            Work work,
            BasicInfoSnapshot snapshot) {

        if (!Objects.equals(
                    work.getName(),
                    snapshot.name())
                || work.getCategory()
                    != snapshot.category()
                || !Objects.equals(
                    work.getPhotoReference(),
                    snapshot.photoReference())) {

            throw new BasicInfoConflictException(
                    work.getId()
            );
        }
    }

    private void cleanupCandidateImage(
            String photoReference,
            RuntimeException originalException) {

        if (photoReference == null) {
            return;
        }

        try {
            workImageStorage.deleteImage(
                    photoReference
            );
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(
                    cleanupException
            );
        }
    }

    private void deleteOldImageBestEffort(
            String photoReference) {

        if (photoReference == null) {
            return;
        }

        try {
            workImageStorage.deleteImage(
                    photoReference
            );
        } catch (RuntimeException e) {
            logger.warn(
                    "Failed to delete old work image after database commit.",
                    e
            );
        }
    }

    @Transactional(readOnly = true)
    public Page<InProgressWorkSummary> getInProgressWorks(
            Long userId,
            WorkCategory category,
            int pageIndex,
            int pageSize) {

        User user = getAuthenticatedUser(userId);

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(
                        Sort.Order.desc("lastUpdatedAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<Work> works;

        if (category == null) {
            works = workRepository.findByUserAndStatus(
                    user,
                    WorkStatus.IN_PROGRESS,
                    pageable
            );
        } else {
            works = workRepository.findByUserAndStatusAndCategory(
                    user,
                    WorkStatus.IN_PROGRESS,
                    category,
                    pageable
            );
        }

        return works.map(
                this::toInProgressWorkSummary
        );
    }

    @Transactional(readOnly = true)
    public Page<CompletedWorkSummary> getCompletedWorks(
            Long userId,
            WorkCategory category,
            int pageIndex) {

        User user = getAuthenticatedUser(userId);

        Pageable pageable = PageRequest.of(
                pageIndex,
                10,
                Sort.by(
                        Sort.Order.desc("completionDate"),
                        Sort.Order.desc("id")
                )
        );

        Page<Work> works;

        if (category == null) {
            works = workRepository.findByUserAndStatus(
                    user,
                    WorkStatus.COMPLETED,
                    pageable
            );
        } else {
            works = workRepository.findByUserAndStatusAndCategory(
                    user,
                    WorkStatus.COMPLETED,
                    category,
                    pageable
            );
        }

        return works.map(
                this::toCompletedWorkSummary
        );
    }

    @Transactional(readOnly = true)
    public WorkDetailResult getWorkDetail(
            Long userId,
            Long workId) {

        Work work = getOwnedWork(
                userId,
                workId
        );

        String submissionToken =
                submissionTokenManager
                        .issueBasicInfoToken(
                                work.getId(),
                                work.getName(),
                                work.getCategory(),
                                work.getPhotoReference()
                        );

        return toWorkDetailResult(
                work,
                submissionToken
        );
    }

    @Transactional(readOnly = true)
    public WorkDetailResult getWorkDetailForRedisplay(
            Long userId,
            Long workId,
            String existingSubmissionToken) {

        Work work = getOwnedWork(
                userId,
                workId
        );

        return toWorkDetailResult(
                work,
                existingSubmissionToken
        );
    }

    @Transactional(readOnly = true)
    public WorkImageResult getWorkImage(
            Long userId,
            Long workId) {

        Work work = getOwnedWork(
                userId,
                workId
        );

        String photoReference =
                work.getPhotoReference();

        if (photoReference == null) {
            return new WorkImageResult(
                    workImageStorage.readDefaultImage(),
                    "image/png"
            );
        }

        Resource resource =
                workImageStorage.readImage(
                        photoReference
                );

        if (resource == null) {
            return new WorkImageResult(
                    workImageStorage.readDefaultImage(),
                    "image/png"
            );
        }

        return new WorkImageResult(
                resource,
                workImageStorage.determineContentType(
                        photoReference
                )
        );
    }

    public Long createWork(
            Long userId,
            String name,
            MultipartFile photo,
            WorkCategory category,
            WorkStatus status,
            String submissionToken) {

        submissionTokenManager.beginWorkCreate(
                submissionToken
        );

        String candidatePhotoReference = null;
        Long workId;

        try {
            candidatePhotoReference =
                    saveCandidateImage(photo);

            workId =
                    saveWorkInTransaction(
                            userId,
                            name,
                            candidatePhotoReference,
                            category,
                            status
                    );

        } catch (RuntimeException e) {

            cleanupCandidateImage(
                    candidatePhotoReference,
                    e
            );

            try {
                submissionTokenManager.releaseForRetry(
                        submissionToken
                );
            } catch (RuntimeException releaseException) {
                e.addSuppressed(
                        releaseException
                );
            }

            throw e;
        }

        submissionTokenManager.complete(
                submissionToken
        );

        return workId;
    }

    public void updateBasicInfo(
            Long userId,
            Long workId,
            String name,
            MultipartFile photo,
            WorkCategory category,
            boolean deletePhoto,
            String submissionToken) {

        BasicInfoSnapshot snapshot =
                submissionTokenManager
                        .beginBasicInfo(
                                submissionToken,
                                workId
                        );

        String candidatePhotoReference = null;

        try {
            transactionTemplate.execute(
                    transactionStatus -> {

                        Work work = getOwnedWork(
                                userId,
                                workId
                        );

                        verifyBasicInfoSnapshot(
                                work,
                                snapshot
                        );

                        return null;
                    }
            );

            candidatePhotoReference =
                    saveCandidateImage(photo);

            String newPhotoReference;

            if (candidatePhotoReference != null) {
                newPhotoReference =
                        candidatePhotoReference;

            } else if (deletePhoto) {
                newPhotoReference = null;

            } else {
                newPhotoReference =
                        snapshot.photoReference();
            }

            final String finalPhotoReference =
                    newPhotoReference;

            String oldPhotoReference =
                    transactionTemplate.execute(
                            transactionStatus -> {

                                Work work =
                                        getOwnedWorkForUpdate(
                                                userId,
                                                workId
                                        );

                                verifyBasicInfoSnapshot(
                                        work,
                                        snapshot
                                );

                                String oldReference =
                                        work.getPhotoReference();

                                boolean changed =
                                        !Objects.equals(
                                                work.getName(),
                                                name
                                        )
                                        || work.getCategory()
                                                != category
                                        || !Objects.equals(
                                                work.getPhotoReference(),
                                                finalPhotoReference
                                        );

                                if (changed) {
                                    work.updateBasicInfo(
                                            name,
                                            finalPhotoReference,
                                            category,
                                            LocalDateTime.now(
                                                    clock
                                            )
                                    );
                                }

                                return oldReference;
                            }
                    );

            submissionTokenManager.complete(
                    submissionToken
            );

            if (!Objects.equals(
                    oldPhotoReference,
                    finalPhotoReference)) {

                deleteOldImageBestEffort(
                        oldPhotoReference
                );
            }

        } catch (BasicInfoConflictException e) {

            cleanupCandidateImage(
                    candidatePhotoReference,
                    e
            );

            submissionTokenManager.invalidate(
                    submissionToken
            );

            throw e;

        } catch (WorkNotFoundException e) {

            cleanupCandidateImage(
                    candidatePhotoReference,
                    e
            );

            submissionTokenManager.invalidate(
                    submissionToken
            );

            throw e;

        } catch (RuntimeException e) {

            cleanupCandidateImage(
                    candidatePhotoReference,
                    e
            );

            try {
                submissionTokenManager.releaseForRetry(
                        submissionToken
                );
            } catch (RuntimeException releaseException) {
                e.addSuppressed(
                        releaseException
                );
            }

            throw e;
        }
    }

    public void deleteWork(
            Long userId,
            Long workId) {

        String oldPhotoReference =
                transactionTemplate.execute(
                        transactionStatus -> {

                            Work work =
                                    getOwnedWorkForUpdate(
                                            userId,
                                            workId
                                    );

                            String photoReference =
                                    work.getPhotoReference();

                            workRepository.delete(work);

                            return photoReference;
                        }
                );

        deleteOldImageBestEffort(
                oldPhotoReference
        );
    }
}