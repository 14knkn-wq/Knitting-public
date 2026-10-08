package com.example.demo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.domain.CounterType;
import com.example.demo.dto.CompletionDateSaveResult;
import com.example.demo.dto.CounterSaveResult;
import com.example.demo.dto.StatusSaveResult;
import com.example.demo.entity.WorkStatus;
import com.example.demo.exception.AutoSaveConflictException;
import com.example.demo.exception.InvalidCompletionDateException;
import com.example.demo.exception.InvalidCompletionDateException.Reason;
import com.example.demo.exception.WorkNotFoundException;
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.WorkAutoSaveService;

@WebMvcTest(WorkAutoSaveController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkAutoSaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkAutoSaveService workAutoSaveService;

    @BeforeEach
    void setUp() {

        KnittingUserPrincipal principal =
                new KnittingUserPrincipal(
                        1L,
                        "autosaveuser",
                        "{bcrypt}dummy"
                );

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContext context =
                SecurityContextHolder
                        .createEmptyContext();

        context.setAuthentication(
                authentication
        );

        SecurityContextHolder.setContext(
                context
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void カウンターを正常保存すると200と保存結果を返す()
            throws Exception {

        when(
                workAutoSaveService.updateCounter(
                        1L,
                        100L,
                        CounterType.STITCH,
                        11,
                        10
                )
        ).thenReturn(
                new CounterSaveResult(
                        CounterType.STITCH,
                        11
                )
        );

        mockMvc.perform(
                post("/works/100/counter")
                        .param(
                                "counterType",
                                "STITCH"
                        )
                        .param(
                                "value",
                                "11"
                        )
                        .param(
                                "expectedValue",
                                "10"
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.counterType"
                        ).value("STITCH")
                )
                .andExpect(
                        jsonPath(
                                "$.value"
                        ).value(11)
                );

        verify(workAutoSaveService)
                .updateCounter(
                        1L,
                        100L,
                        CounterType.STITCH,
                        11,
                        10
                );
    }

    @Test
    void カウンター範囲外では400になりServiceを呼ばない()
            throws Exception {

        mockMvc.perform(
                post("/works/100/counter")
                        .param(
                                "counterType",
                                "STITCH"
                        )
                        .param(
                                "value",
                                "10000"
                        )
                        .param(
                                "expectedValue",
                                "10"
                        )
        )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        ).value(
                                "VALIDATION_ERROR"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.fieldErrors.value"
                        ).exists()
                );

        verifyNoInteractions(
                workAutoSaveService
        );
    }

    @Test
    void カウンター数値型変換失敗では400になる()
            throws Exception {

        mockMvc.perform(
                post("/works/100/counter")
                        .param(
                                "counterType",
                                "STITCH"
                        )
                        .param(
                                "value",
                                "abc"
                        )
                        .param(
                                "expectedValue",
                                "10"
                        )
        )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        ).value(
                                "VALIDATION_ERROR"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.fieldErrors.value"
                        ).value(
                                "0～9999の整数で入力してください。"
                        )
                );

        verifyNoInteractions(
                workAutoSaveService
        );
    }

    @Test
    void メモを正常保存すると200で本文なし()
            throws Exception {

        mockMvc.perform(
                post("/works/100/memo")
                        .param(
                                "memo",
                                "新しいメモ"
                        )
                        .param(
                                "expectedMemo",
                                "以前のメモ"
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("")
                );

        verify(workAutoSaveService)
                .updateMemo(
                        1L,
                        100L,
                        "新しいメモ",
                        "以前のメモ"
                );
    }

    @Test
    void ステータスを正常保存すると200とサーバー確定完成日を返す()
            throws Exception {

        LocalDate completionDate =
                LocalDate.of(
                        2026,
                        10,
                        7
                );

        when(
                workAutoSaveService.updateStatus(
                        1L,
                        100L,
                        WorkStatus.COMPLETED,
                        WorkStatus.IN_PROGRESS,
                        null
                )
        ).thenReturn(
                new StatusSaveResult(
                        WorkStatus.COMPLETED,
                        completionDate
                )
        );

        mockMvc.perform(
                post("/works/100/status")
                        .param(
                                "status",
                                "COMPLETED"
                        )
                        .param(
                                "expectedStatus",
                                "IN_PROGRESS"
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        ).value("COMPLETED")
                )
                .andExpect(
                        jsonPath(
                                "$.completionDate"
                        ).value("2026-10-07")
                );
    }

    @Test
    void 完成日を正常保存すると200と確定日を返す()
            throws Exception {

        LocalDate oldDate =
                LocalDate.of(
                        2026,
                        10,
                        5
                );

        LocalDate newDate =
                LocalDate.of(
                        2026,
                        10,
                        6
                );

        when(
                workAutoSaveService
                        .updateCompletionDate(
                                1L,
                                100L,
                                newDate,
                                WorkStatus.COMPLETED,
                                oldDate
                        )
        ).thenReturn(
                new CompletionDateSaveResult(
                        newDate
                )
        );

        mockMvc.perform(
                post(
                        "/works/100/completion-date"
                )
                        .param(
                                "completionDate",
                                "2026-10-06"
                        )
                        .param(
                                "expectedStatus",
                                "COMPLETED"
                        )
                        .param(
                                "expectedCompletionDate",
                                "2026-10-05"
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.completionDate"
                        ).value("2026-10-06")
                );
    }

    @Test
    void 自動保存競合では409とCONFLICTを返す()
            throws Exception {

        when(
                workAutoSaveService.updateCounter(
                        1L,
                        100L,
                        CounterType.STITCH,
                        11,
                        10
                )
        ).thenThrow(
                new AutoSaveConflictException()
        );

        mockMvc.perform(
                post("/works/100/counter")
                        .param(
                                "counterType",
                                "STITCH"
                        )
                        .param(
                                "value",
                                "11"
                        )
                        .param(
                                "expectedValue",
                                "10"
                        )
        )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        ).value("CONFLICT")
                )
                .andExpect(
                        jsonPath(
                                "$.fieldErrors"
                        ).doesNotExist()
                );
    }

    @Test
    void 未来日の業務エラーでは完成日の400ValidationErrorを返す()
            throws Exception {

        LocalDate currentDate =
                LocalDate.of(
                        2026,
                        10,
                        7
                );

        LocalDate futureDate =
                LocalDate.of(
                        2026,
                        10,
                        8
                );

        when(
                workAutoSaveService
                        .updateCompletionDate(
                                1L,
                                100L,
                                futureDate,
                                WorkStatus.COMPLETED,
                                currentDate
                        )
        ).thenThrow(
                new InvalidCompletionDateException(
                        Reason.FUTURE_DATE
                )
        );

        mockMvc.perform(
                post(
                        "/works/100/completion-date"
                )
                        .param(
                                "completionDate",
                                "2026-10-08"
                        )
                        .param(
                                "expectedStatus",
                                "COMPLETED"
                        )
                        .param(
                                "expectedCompletionDate",
                                "2026-10-07"
                        )
        )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.code"
                        ).value(
                                "VALIDATION_ERROR"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.fieldErrors.completionDate"
                        ).value(
                                "未来の日付は指定できません。"
                        )
                );
    }

    @Test
    void 本人所有作品を取得できない場合は404で本文なし()
            throws Exception {

        when(
                workAutoSaveService.updateStatus(
                        1L,
                        999L,
                        WorkStatus.COMPLETED,
                        WorkStatus.IN_PROGRESS,
                        null
                )
        ).thenThrow(
                new WorkNotFoundException()
        );

        mockMvc.perform(
                post("/works/999/status")
                        .param(
                                "status",
                                "COMPLETED"
                        )
                        .param(
                                "expectedStatus",
                                "IN_PROGRESS"
                        )
        )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        content().string("")
                );
    }
}