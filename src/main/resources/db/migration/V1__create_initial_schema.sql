CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uq_users_email
        UNIQUE (email),

    CONSTRAINT uq_users_username
        UNIQUE (username)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


CREATE TABLE works (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    photo_reference VARCHAR(500) NULL,
    category VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    stitch_count INT NOT NULL DEFAULT 0,
    row_count INT NOT NULL DEFAULT 0,
    memo VARCHAR(1000) NOT NULL DEFAULT '',
    completion_date DATE NULL,
    last_updated_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_works_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT chk_works_category
        CHECK (
            category IN (
                'HAT',
                'TOP',
                'BOTTOM',
                'SCARF',
                'BAG',
                'TOY',
                'SMALL_GOODS',
                'OTHER'
            )
        ),

    CONSTRAINT chk_works_status
        CHECK (
            status IN (
                'IN_PROGRESS',
                'COMPLETED'
            )
        ),

    CONSTRAINT chk_works_stitch_count
        CHECK (
            stitch_count BETWEEN 0 AND 9999
        ),

    CONSTRAINT chk_works_row_count
        CHECK (
            row_count BETWEEN 0 AND 9999
        ),

    CONSTRAINT chk_works_status_completion_date
        CHECK (
            (status = 'IN_PROGRESS' AND completion_date IS NULL)
            OR
            (status = 'COMPLETED' AND completion_date IS NOT NULL)
        ),

    INDEX idx_works_user_status_updated (
        user_id,
        status,
        last_updated_at DESC,
        id DESC
    ),

    INDEX idx_works_user_status_completed (
        user_id,
        status,
        completion_date DESC,
        id DESC
    )
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;