package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.InProgressWorkSummary;
import com.example.demo.entity.User;
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkRepository;

@Service
public class WorkService {

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

            if (candidatePhotoReference != null) {
                try {
                    workImageStorage.deleteImage(
                            candidatePhotoReference
                    );
                } catch (RuntimeException cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }

            try {
                submissionTokenManager.releaseForRetry(
                        submissionToken
                );
            } catch (RuntimeException releaseException) {
                e.addSuppressed(releaseException);
            }

            throw e;
        }

        submissionTokenManager.complete(
                submissionToken
        );

        return workId;
    }
}