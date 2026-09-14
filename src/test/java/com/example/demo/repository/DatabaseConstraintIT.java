package com.example.demo.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.entity.User;
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles("test")
class DatabaseConstraintIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkRepository workRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 大文字小文字だけ異なるusernameは重複登録できない() {

        User firstUser = new User(
            "first@example.com",
            "Mame123",
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 9, 12, 12, 0)
        );

        userRepository.saveAndFlush(firstUser);

        User duplicateUser = new User(
            "second@example.com",
            "mame123",
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 9, 12, 12, 1)
        );

        assertThatThrownBy(
            () -> userRepository.saveAndFlush(duplicateUser)
        )
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 同じemailは重複登録できない() {

        User firstUser = new User(
            "duplicate@example.com",
            "User111",
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 9, 12, 12, 0)
        );

        userRepository.saveAndFlush(firstUser);

        User duplicateUser = new User(
            "duplicate@example.com",
            "User222",
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 9, 12, 12, 1)
        );

        assertThatThrownBy(
            () -> userRepository.saveAndFlush(duplicateUser)
        )
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void stitchCountが10000なら保存できない() {

        User owner = createUser(
            "stitch@example.com",
            "Stitch123"
        );

        assertThatThrownBy(() ->
            entityManager.createNativeQuery("""
                INSERT INTO works (
                    user_id,
                    name,
                    category,
                    status,
                    stitch_count,
                    row_count,
                    memo,
                    completion_date,
                    last_updated_at
                )
                VALUES (
                    :userId,
                    '目数上限テスト',
                    'HAT',
                    'IN_PROGRESS',
                    10000,
                    0,
                    '',
                    NULL,
                    :lastUpdatedAt
                )
                """)
                .setParameter("userId", owner.getId())
                .setParameter(
                    "lastUpdatedAt",
                    LocalDateTime.of(2026, 9, 12, 13, 0)
                )
                .executeUpdate()
        )
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    void stitchCountが負数なら保存できない() {

        User owner = createUser(
            "negative@example.com",
            "Negative123"
        );

        assertThatThrownBy(() ->
            entityManager.createNativeQuery("""
                INSERT INTO works (
                    user_id,
                    name,
                    category,
                    status,
                    stitch_count,
                    row_count,
                    memo,
                    completion_date,
                    last_updated_at
                )
                VALUES (
                    :userId,
                    '目数下限テスト',
                    'HAT',
                    'IN_PROGRESS',
                    -1,
                    0,
                    '',
                    NULL,
                    :lastUpdatedAt
                )
                """)
                .setParameter("userId", owner.getId())
                .setParameter(
                    "lastUpdatedAt",
                    LocalDateTime.of(2026, 9, 12, 13, 0)
                )
                .executeUpdate()
        )
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    void 完成なのに完成日がnullなら保存できない() {

        User owner = createUser(
            "completed@example.com",
            "Complete123"
        );

        Work work = new Work(
            owner,
            "完成日制約テスト",
            null,
            WorkCategory.SCARF,
            WorkStatus.COMPLETED,
            null,
            LocalDateTime.of(2026, 9, 12, 13, 0)
        );

        assertThatThrownBy(
            () -> workRepository.saveAndFlush(work)
        )
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 存在しないuserIdでは作品を保存できない() {

        User user = createUser(
            "foreign@example.com",
            "Foreign123"
        );

        Long deletedUserId = user.getId();

        userRepository.delete(user);
        userRepository.flush();

        assertThatThrownBy(() ->
            entityManager.createNativeQuery("""
                INSERT INTO works (
                    user_id,
                    name,
                    category,
                    status,
                    stitch_count,
                    row_count,
                    memo,
                    completion_date,
                    last_updated_at
                )
                VALUES (
                    :userId,
                    '外部キー制約テスト',
                    'BAG',
                    'IN_PROGRESS',
                    0,
                    0,
                    '',
                    NULL,
                    :lastUpdatedAt
                )
                """)
                .setParameter("userId", deletedUserId)
                .setParameter(
                    "lastUpdatedAt",
                    LocalDateTime.of(2026, 9, 12, 13, 0)
                )
                .executeUpdate()
        )
            .isInstanceOf(PersistenceException.class);
    }

    private User createUser(
            String email,
            String username
    ) {

        User user = new User(
            email,
            username,
            "{bcrypt}dummy",
            LocalDateTime.of(2026, 9, 12, 12, 0)
        );

        return userRepository.saveAndFlush(user);
    }
}