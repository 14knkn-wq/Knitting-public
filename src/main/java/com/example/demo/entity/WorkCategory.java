package com.example.demo.entity;

public enum WorkCategory {

    HAT("帽子"),
    TOP("上衣"),
    BOTTOM("下衣"),
    SCARF("マフラー"),
    BAG("バッグ"),
    TOY("ぬいぐるみ"),
    SMALL_GOODS("小物"),
    OTHER("その他");

    private final String displayName;

    WorkCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}