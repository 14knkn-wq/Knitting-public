package com.example.demo.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dto.CompletedWorkSummary;
import com.example.demo.entity.WorkCategory;
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.SubmissionTokenManager;
import com.example.demo.service.WorkService;

@WebMvcTest(WorkController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkCompletedViewTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkService workService;

    @MockitoBean
    private SubmissionTokenManager submissionTokenManager;

    @BeforeEach
    void setUpAuthentication() {

        KnittingUserPrincipal principal =
                new KnittingUserPrincipal(
                        1L,
                        "completedviewuser",
                        "{bcrypt}dummy"
                );

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
    }

    @Test
    void 完成作品カードと画面の基本要素を表示する()
            throws Exception {

        CompletedWorkSummary noPhotoWork =
                new CompletedWorkSummary(
                        101L,
                        "写真なし完成作品",
                        LocalDate.of(
                                2026,
                                9,
                                28
                        ),
                        false
                );

        CompletedWorkSummary photoWork =
                new CompletedWorkSummary(
                        102L,
                        "写真あり完成作品",
                        LocalDate.of(
                                2026,
                                9,
                                29
                        ),
                        true
                );

        var worksPage =
                new PageImpl<>(
                        List.of(
                                photoWork,
                                noPhotoWork
                        ),
                        PageRequest.of(0, 10),
                        2
                );

        when(
                workService.getCompletedWorks(
                        1L,
                        null,
                        0
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/completed")
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "works/completed"
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "▶ 完成一覧"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/in-progress"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "＋ 作品を登録"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "写真なし完成作品"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "写真あり完成作品"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "完成日："
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "2026/09/28"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "2026/09/29"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/images/default-work.png"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/102/image"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/101?from=completed"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/102?from=completed"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "work-category-filter"
                                )
                        )
                );
    }

    @Test
    void 完成作品が0件なら全体0件メッセージと登録導線を表示する()
            throws Exception {

        var worksPage =
                new PageImpl<CompletedWorkSummary>(
                        List.of(),
                        PageRequest.of(0, 10),
                        0
                );

        when(
                workService.getCompletedWorks(
                        1L,
                        null,
                        0
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/completed")
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "完成した作品はありません。"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "作品を登録する"
                                )
                        )
                );
    }

    @Test
    void カテゴリー絞り込み結果が0件なら専用メッセージを表示する()
            throws Exception {

        var worksPage =
                new PageImpl<CompletedWorkSummary>(
                        List.of(),
                        PageRequest.of(0, 10),
                        0
                );

        when(
                workService.getCompletedWorks(
                        1L,
                        WorkCategory.HAT,
                        0
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/completed")
                        .param(
                                "category",
                                "HAT"
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "このカテゴリーの完成作品はありません。"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "すべて表示"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "value=\"HAT\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "selected=\"selected\""
                                )
                        )
                );
    }

    @Test
    void 完成一覧で10件以下ならページ切り替えを表示しない()
            throws Exception {

        CompletedWorkSummary work =
                new CompletedWorkSummary(
                        201L,
                        "完成作品",
                        LocalDate.of(
                                2026,
                                9,
                                29
                        ),
                        false
                );

        var worksPage =
                new PageImpl<>(
                        List.of(work),
                        PageRequest.of(0, 10),
                        10
                );

        when(
                workService.getCompletedWorks(
                        1L,
                        null,
                        0
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/completed")
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "aria-label=\"ページ切り替え\""
                                        )
                                )
                        )
                );
    }

    @Test
    void 完成一覧で11件以上ならページ切り替えを表示する()
    		throws Exception {

        CompletedWorkSummary work =
                new CompletedWorkSummary(
                        301L,
                        "1ページ目作品",
                        LocalDate.of(
                                2026,
                                9,
                                29
                        ),
                        false
                );

        var worksPage =
                new PageImpl<>(
                        List.of(work),
                        PageRequest.of(0, 10),
                        11
                );

        when(
                workService.getCompletedWorks(
                        1L,
                        null,
                        0
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/completed")
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "aria-label=\"ページ切り替え\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/completed?page=2"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "次へ"
                                )
                        )
                );
    }

    @Test
    void ページ切り替え時はカテゴリーを維持し1ページ目ではpageを省略する()
            throws Exception {

        CompletedWorkSummary work =
                new CompletedWorkSummary(
                        401L,
                        "帽子の完成作品",
                        LocalDate.of(
                                2026,
                                9,
                                29
                        ),
                        false
                );

        var worksPage =
                new PageImpl<>(
                        List.of(work),
                        PageRequest.of(1, 10),
                        11
                );

        when(
                workService.getCompletedWorks(
                        1L,
                        WorkCategory.HAT,
                        1
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/completed")
                        .param(
                                "category",
                                "HAT"
                        )
                        .param(
                                "page",
                                "2"
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/completed?category=HAT"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "page=1"
                                        )
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "前へ"
                                )
                        )
                );
    }
}