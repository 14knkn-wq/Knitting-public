package com.example.demo.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

import com.example.demo.dto.InProgressWorkSummary;
import com.example.demo.entity.WorkCategory;
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.SubmissionTokenManager;
import com.example.demo.service.WorkService;

@WebMvcTest(WorkController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkInProgressViewTest {

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
                        "viewuser",
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
    void 制作中作品カードに必要な情報とリンクを表示する()
            throws Exception {

        InProgressWorkSummary noPhotoWork =
                new InProgressWorkSummary(
                        101L,
                        "写真なし作品",
                        25,
                        40,
                        false
                );

        InProgressWorkSummary photoWork =
                new InProgressWorkSummary(
                        102L,
                        "写真あり作品",
                        50,
                        60,
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
                workService.getInProgressWorks(
                        1L,
                        null,
                        0,
                        10
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/in-progress")
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "works/in-progress"
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/101?from=in-progress"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/102?from=in-progress"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "写真なし作品"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "写真あり作品"
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
                                containsString("25")
                        )
                )
                .andExpect(
                        content().string(
                                containsString("40")
                        )
                )
                .andExpect(
                        content().string(
                                containsString("50")
                        )
                )
                .andExpect(
                        content().string(
                                containsString("60")
                        )
                );
    }

    @Test
    void カテゴリー絞り込み結果が0件なら専用メッセージを表示する()
            throws Exception {

        var worksPage =
                new PageImpl<InProgressWorkSummary>(
                        List.of(),
                        PageRequest.of(0, 10),
                        0
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        WorkCategory.HAT,
                        0,
                        10
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/in-progress")
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
                                        "このカテゴリーの制作中作品はありません。"
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
    void 複数ページがある場合はページ切り替えを表示する()
            throws Exception {

        InProgressWorkSummary work =
                new InProgressWorkSummary(
                        201L,
                        "1ページ目作品",
                        1,
                        1,
                        false
                );

        var worksPage =
                new PageImpl<>(
                        List.of(work),
                        PageRequest.of(0, 10),
                        11
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        null,
                        0,
                        10
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/in-progress")
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
                                        "/works/in-progress?page=2"
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
    void ページ切り替え時は選択カテゴリーを維持する()
            throws Exception {

        InProgressWorkSummary work =
                new InProgressWorkSummary(
                        301L,
                        "帽子作品",
                        1,
                        1,
                        false
                );

        var worksPage =
                new PageImpl<>(
                        List.of(work),
                        PageRequest.of(1, 10),
                        11
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        WorkCategory.HAT,
                        1,
                        10
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/in-progress")
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
                                        "/works/in-progress?category=HAT"
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

    @Test
    void 総ページ数が8以上なら省略記号を表示する()
            throws Exception {

        InProgressWorkSummary work =
                new InProgressWorkSummary(
                        401L,
                        "中央ページ作品",
                        1,
                        1,
                        false
                );

        var worksPage =
                new PageImpl<>(
                        List.of(work),
                        PageRequest.of(4, 10),
                        80
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        null,
                        4,
                        10
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/works/in-progress")
                        .param(
                                "page",
                                "5"
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                containsString("…")
                        )
                );
    }


}