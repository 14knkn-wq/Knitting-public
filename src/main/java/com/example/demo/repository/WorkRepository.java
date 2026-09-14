package com.example.demo.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.example.demo.entity.User;
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;

public interface WorkRepository extends JpaRepository<Work, Long> {

    Optional<Work> findByIdAndUser(
        Long id,
        User user
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Work> findByIdAndUserId(
        Long id,
        Long userId
    );

    Page<Work> findByUserAndStatus(
        User user,
        WorkStatus status,
        Pageable pageable
    );

    Page<Work> findByUserAndStatusAndCategory(
        User user,
        WorkStatus status,
        WorkCategory category,
        Pageable pageable
    );
}