package com.example.demo.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.WorkService;

@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeViewTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkService workService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 未ログインでは制作中作品を表示せず公開機能と認証導線を表示する()
            throws Exception {

        mockMvc.perform(
                get("/")
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name("home")
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "制作中の作品"
                                        )
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/gauge-adjustment"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/yarn-combination"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/counter"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/yarn-guide"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/login"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/register"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "global-menu-button"
                                        )
                                )
                        )
                );

        verifyNoInteractions(
                workService
        );
    }

    @Test
    void ログイン済みでは制作中作品と各導線を表示する()
            throws Exception {

        var authentication =
                setAuthentication();

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
                        PageRequest.of(0, 4),
                        2
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        null,
                        0,
                        4
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/")
                        .principal(authentication)
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name("home")
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "制作中の作品"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "▶ トップ"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "href=\"/works/101\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "href=\"/works/102\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "?from=in-progress"
                                        )
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
                                        "/works/new"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "制作中一覧を見る"
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
                                        "/gauge-adjustment"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/yarn-combination"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/counter"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/yarn-guide"
                                )
                        )
                );
    }

    @Test
    void ログイン済みで制作中作品が0件なら空状態と必要な導線を表示する()
            throws Exception {

        var authentication =
                setAuthentication();

        var worksPage =
                new PageImpl<InProgressWorkSummary>(
                        List.of(),
                        PageRequest.of(0, 4),
                        0
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        null,
                        0,
                        4
                )
        ).thenReturn(worksPage);

        mockMvc.perform(
                get("/")
                        .principal(authentication)
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name("home")
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "制作中の作品はありません。"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "作品を登録する"
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
                                        "制作中一覧を見る"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/new"
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
                                not(
                                        containsString(
                                                "/images/default-work.png"
                                        )
                                )
                        )
                );
    }

    private UsernamePasswordAuthenticationToken setAuthentication() {

        KnittingUserPrincipal principal =
                new KnittingUserPrincipal(
                        1L,
                        "homeuser",
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

        context.setAuthentication(
                authentication
        );

        SecurityContextHolder.setContext(
                context
        );

        return authentication;
    }
}