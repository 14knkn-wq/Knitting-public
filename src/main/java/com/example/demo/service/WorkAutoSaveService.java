package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.CounterType;
import com.example.demo.dto.CompletionDateSaveResult;
import com.example.demo.dto.CounterSaveResult;
import com.example.demo.dto.StatusSaveResult;
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkStatus;
import com.example.demo.exception.AutoSaveConflictException;
import com.example.demo.exception.InvalidCompletionDateException;
import com.example.demo.exception.InvalidCompletionDateException.Reason;
import com.example.demo.exception.WorkNotFoundException;
import com.example.demo.repository.WorkRepository;

@Service
public class WorkAutoSaveService {

    private final WorkRepository workRepository;
    private final Clock clock;

    public WorkAutoSaveService(
            WorkRepository workRepository,
            Clock clock) {

        this.workRepository = workRepository;
        this.clock = clock;
    }

    @Transactional
    public CounterSaveResult updateCounter(
            Long userId,
            Long workId,
            CounterType counterType,
            int value,
            int expectedValue) {

        Work work = getOwnedWorkForUpdate(
                userId,
                workId
        );

        int currentValue = switch (counterType) {
            case STITCH -> work.getStitchCount();
            case ROW -> work.getRowCount();
        };

        if (currentValue != expectedValue) {
            throw new AutoSaveConflictException();
        }

        if (currentValue == value) {
            return new CounterSaveResult(
                    counterType,
                    currentValue
            );
        }

        LocalDateTime updatedAt =
                LocalDateTime.now(clock);

        switch (counterType) {
            case STITCH ->
                work.updateStitchCount(
                        value,
                        updatedAt
                );

            case ROW ->
                work.updateRowCount(
                        value,
                        updatedAt
                );
        }

        return new CounterSaveResult(
                counterType,
                value
        );
    }

    @Transactional
    public void updateMemo(
            Long userId,
            Long workId,
            String memo,
            String expectedMemo) {

        Work work = getOwnedWorkForUpdate(
                userId,
                workId
        );

        if (!Objects.equals(
                work.getMemo(),
                expectedMemo)) {
            throw new AutoSaveConflictException();
        }

        if (Objects.equals(
                work.getMemo(),
                memo)) {
            return;
        }

        work.updateMemo(
                memo,
                LocalDateTime.now(clock)
        );
    }

    @Transactional
    public StatusSaveResult updateStatus(
            Long userId,
            Long workId,
            WorkStatus status,
            WorkStatus expectedStatus,
            LocalDate expectedCompletionDate) {

        Work work = getOwnedWorkForUpdate(
                userId,
                workId
        );

        if (work.getStatus() != expectedStatus
                || !Objects.equals(
                        work.getCompletionDate(),
                        expectedCompletionDate)) {
            throw new AutoSaveConflictException();
        }

        if (work.getStatus() == status) {
            return new StatusSaveResult(
                    work.getStatus(),
                    work.getCompletionDate()
            );
        }

        LocalDate completionDate =
                status == WorkStatus.COMPLETED
                        ? LocalDate.now(clock)
                        : null;

        work.updateStatus(
                status,
                completionDate,
                LocalDateTime.now(clock)
        );

        return new StatusSaveResult(
                status,
                completionDate
        );
    }

    @Transactional
    public CompletionDateSaveResult updateCompletionDate(
            Long userId,
            Long workId,
            LocalDate completionDate,
            WorkStatus expectedStatus,
            LocalDate expectedCompletionDate) {

        Work work = getOwnedWorkForUpdate(
                userId,
                workId
        );

        if (work.getStatus() != expectedStatus
                || !Objects.equals(
                        work.getCompletionDate(),
                        expectedCompletionDate)) {
            throw new AutoSaveConflictException();
        }

        if (work.getStatus()
                != WorkStatus.COMPLETED) {
            throw new InvalidCompletionDateException(
                    Reason.WORK_NOT_COMPLETED
            );
        }

        LocalDate today = LocalDate.now(clock);

        if (completionDate.isAfter(today)) {
            throw new InvalidCompletionDateException(
                    Reason.FUTURE_DATE
            );
        }

        if (Objects.equals(
                work.getCompletionDate(),
                completionDate)) {

            return new CompletionDateSaveResult(
                    work.getCompletionDate()
            );
        }

        work.updateCompletionDate(
                completionDate
        );

        return new CompletionDateSaveResult(
                completionDate
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
}