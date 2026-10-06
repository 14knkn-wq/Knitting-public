package com.example.demo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

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
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.SubmissionTokenManager;
import com.example.demo.service.WorkService;

@WebMvcTest(WorkController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkService workService;

    @MockitoBean
    private SubmissionTokenManager submissionTokenManager;

    @Test
    void 制作中一覧で不正なカテゴリーを指定すると400になる()
            throws Exception {

        mockMvc.perform(
                get("/works/in-progress")
                        .param(
                                "category",
                                "INVALID_CATEGORY"
                        )
        )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        view().name("error/400")
                );

        verifyNoInteractions(workService);
    }

    @Test
    void 制作中一覧でpageに数値以外を指定すると400になる()
            throws Exception {

        mockMvc.perform(
                get("/works/in-progress")
                        .param(
                                "page",
                                "abc"
                        )
        )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        view().name("error/400")
                );

        verifyNoInteractions(workService);
    }

    @Test
    void 完成一覧で不正なカテゴリーを指定すると400になる()
            throws Exception {

        mockMvc.perform(
                get("/works/completed")
                        .param(
                                "category",
                                "INVALID_CATEGORY"
                        )
        )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        view().name("error/400")
                );

        verifyNoInteractions(workService);
    }

    @Test
    void 完成一覧でpageに数値以外を指定すると400になる()
            throws Exception {

        mockMvc.perform(
                get("/works/completed")
                        .param(
                                "page",
                                "abc"
                        )
        )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        view().name("error/400")
                );

        verifyNoInteractions(workService);
    }

    @Test
    void 完成一覧でpage未指定なら1ページ目を表示する()
            throws Exception {

        KnittingUserPrincipal principal =
                new KnittingUserPrincipal(
                        1L,
                        "completedwebuser",
                        "{bcrypt}dummy"
                );

        var authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                authenticationToken
        );

        SecurityContextHolder.setContext(
                context
        );

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
                        view().name(
                                "works/completed"
                        )
                );

        verify(workService)
                .getCompletedWorks(
                        1L,
                        null,
                        0
                );

        SecurityContextHolder.clearContext();
    }
}