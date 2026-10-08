package com.example.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "works")
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    private User user;

    @Column(
        name = "name",
        nullable = false,
        length = 50
    )
    private String name;

    @Column(
        name = "photo_reference",
        length = 500
    )
    private String photoReference;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "category",
        nullable = false,
        length = 20
    )
    private WorkCategory category;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 20
    )
    private WorkStatus status;

    @Column(
        name = "stitch_count",
        nullable = false
    )
    private int stitchCount = 0;

    @Column(
        name = "row_count",
        nullable = false
    )
    private int rowCount = 0;

    @Column(
        name = "memo",
        nullable = false,
        length = 1000
    )
    private String memo = "";

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(
        name = "last_updated_at",
        nullable = false
    )
    private LocalDateTime lastUpdatedAt;

    protected Work() {
    }
    
    public Work(
            User user,
            String name,
            String photoReference,
            WorkCategory category,
            WorkStatus status,
            LocalDate completionDate,
            LocalDateTime lastUpdatedAt
    ) {
        this.user = user;
        this.name = name;
        this.photoReference = photoReference;
        this.category = category;
        this.status = status;
        this.stitchCount = 0;
        this.rowCount = 0;
        this.memo = "";
        this.completionDate = completionDate;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getPhotoReference() {
        return photoReference;
    }

    public WorkCategory getCategory() {
        return category;
    }

    public WorkStatus getStatus() {
        return status;
    }

    public int getStitchCount() {
        return stitchCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public String getMemo() {
        return memo;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void updateBasicInfo(
            String name,
            String photoReference,
            WorkCategory category,
            LocalDateTime lastUpdatedAt
    ) {
        this.name = name;
        this.photoReference = photoReference;
        this.category = category;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void updateStitchCount(
            int stitchCount,
            LocalDateTime lastUpdatedAt
    ) {
        this.stitchCount = stitchCount;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void updateRowCount(
            int rowCount,
            LocalDateTime lastUpdatedAt
    ) {
        this.rowCount = rowCount;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void updateMemo(
            String memo,
            LocalDateTime lastUpdatedAt
    ) {
        this.memo = memo;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void updateStatus(
            WorkStatus status,
            LocalDate completionDate,
            LocalDateTime lastUpdatedAt
    ) {
        this.status = status;
        this.completionDate = completionDate;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void updateCompletionDate(
            LocalDate completionDate
    ) {
        this.completionDate = completionDate;
    }
}