package com.example.demo.entity;

public enum WorkStatus {

    IN_PROGRESS("制作中"),
    COMPLETED("完成");

    private final String displayName;

    WorkStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}