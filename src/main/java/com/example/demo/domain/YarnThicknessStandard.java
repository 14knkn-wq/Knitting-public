package com.example.demo.domain;

public enum YarnThicknessStandard {

    GOKUBOSO(
            "極細",
            800,
            null,
            "800m以上",
            "0～2号",
            "2.1～2.7mm",
            "2/0号",
            "2.0mm"
    ),

    AIBOSO(
            "合細",
            500,
            799L,
            "500～799m",
            "1～3号",
            "2.4～3.0mm",
            "2/0～3/0号",
            "2.0～2.3mm"
    ),

    CHUBOSO(
            "中細",
            350,
            499L,
            "350～499m",
            "2～4号",
            "2.7～3.3mm",
            "2/0～4/0号",
            "2.0～2.5mm"
    ),

    AIBUTO(
            "合太",
            250,
            349L,
            "250～349m",
            "4～6号",
            "3.3～3.9mm",
            "4/0～5/0号",
            "2.5～3.0mm"
    ),

    NAMIBUTO(
            "並太",
            175,
            249L,
            "175～249m",
            "6～8号",
            "3.9～4.5mm",
            "5/0～7/0号",
            "3.0～4.0mm"
    ),

    GOKUBUTO(
            "極太",
            101,
            174L,
            "101～174m",
            "9～11号",
            "4.8～5.4mm",
            "8/0～10/0号",
            "5.0～6.0mm"
    ),

    CHOGOKUBUTO(
            "超極太",
            0,
            100L,
            "100m以下",
            "12号以上",
            "5.7mm以上",
            "10/0号以上",
            "6.0mm以上"
    );

    private final String displayName;
    private final long minLengthPer100g;
    private final Long maxLengthPer100g;
    private final String lengthRangeLabel;
    private final String knittingNeedleNumberLabel;
    private final String knittingNeedleMmLabel;
    private final String crochetHookNumberLabel;
    private final String crochetHookMmLabel;

    YarnThicknessStandard(
            String displayName,
            long minLengthPer100g,
            Long maxLengthPer100g,
            String lengthRangeLabel,
            String knittingNeedleNumberLabel,
            String knittingNeedleMmLabel,
            String crochetHookNumberLabel,
            String crochetHookMmLabel
    ) {
        this.displayName = displayName;
        this.minLengthPer100g = minLengthPer100g;
        this.maxLengthPer100g = maxLengthPer100g;
        this.lengthRangeLabel = lengthRangeLabel;
        this.knittingNeedleNumberLabel = knittingNeedleNumberLabel;
        this.knittingNeedleMmLabel = knittingNeedleMmLabel;
        this.crochetHookNumberLabel = crochetHookNumberLabel;
        this.crochetHookMmLabel = crochetHookMmLabel;
    }

    public static YarnThicknessStandard fromLengthPer100g(long lengthPer100g) {

        if (lengthPer100g < 0) {
            throw new IllegalArgumentException(
                    "lengthPer100g must be 0 or greater."
            );
        }

        for (YarnThicknessStandard standard : values()) {

            boolean meetsLowerBound =
                    lengthPer100g >= standard.minLengthPer100g;

            boolean meetsUpperBound =
                    standard.maxLengthPer100g == null
                            || lengthPer100g <= standard.maxLengthPer100g;

            if (meetsLowerBound && meetsUpperBound) {
                return standard;
            }
        }

        throw new IllegalStateException(
                "No YarnThicknessStandard found."
        );
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getMinLengthPer100g() {
        return minLengthPer100g;
    }

    public Long getMaxLengthPer100g() {
        return maxLengthPer100g;
    }

    public String getLengthRangeLabel() {
        return lengthRangeLabel;
    }

    public String getKnittingNeedleNumberLabel() {
        return knittingNeedleNumberLabel;
    }

    public String getKnittingNeedleMmLabel() {
        return knittingNeedleMmLabel;
    }

    public String getCrochetHookNumberLabel() {
        return crochetHookNumberLabel;
    }

    public String getCrochetHookMmLabel() {
        return crochetHookMmLabel;
    }
}