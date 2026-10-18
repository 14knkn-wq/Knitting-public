package com.example.demo.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
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
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;

import com.example.demo.dto.WorkDetailResult;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.SubmissionTokenManager;
import com.example.demo.service.WorkService;

@WebMvcTest(WorkController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkDetailViewTest {

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
                        "detailuser",
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
    void 制作中作品の詳細画面に必要な要素を表示する()
            throws Exception {

        WorkDetailResult work =
                new WorkDetailResult(
                        100L,
                        "テスト帽子",
                        WorkCategory.HAT,
                        25,
                        40,
                        "ここまで編みました。",
                        WorkStatus.IN_PROGRESS,
                        null,
                        "basic-info-token",
                        false
                );

        when(
                workService.getWorkDetail(
                        1L,
                        100L
                )
        ).thenReturn(work);

        mockMvc.perform(
                get("/works/100")
                        .requestAttr(
                                "_csrf",
                                csrfToken()
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "works/detail"
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "テスト帽子"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "帽子"
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
                                        "value=\"25\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "value=\"40\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "ここまで編みました。"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "id=\"basic-info-form\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "value=\"basic-info-token\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "id=\"stitch-count\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "id=\"row-count\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "id=\"work-memo\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "id=\"work-status-in-progress\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "id=\"work-status-completed\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "id=\"delete-work-dialog\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/100/delete"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/js/work-detail.js"
                                )
                        )
                )
                .andExpect(result -> {

                    String html =
                            result.getResponse()
                                    .getContentAsString();

                    assertEquals(
                            1,
                            StringUtils.countOccurrencesOf(
                                    html,
                                    "<header"
                            )
                    );

                    assertEquals(
                            1,
                            StringUtils.countOccurrencesOf(
                                    html,
                                    "id=\"global-menu-button\""
                            )
                    );

                    assertEquals(
                            1,
                            StringUtils.countOccurrencesOf(
                                    html,
                                    "id=\"global-menu\""
                            )
                    );
                });
    }

    @Test
    void from未指定の制作中作品では制作中一覧を戻り先にする()
            throws Exception {

        WorkDetailResult work =
                createWork(
                        WorkStatus.IN_PROGRESS,
                        null,
                        false
                );

        when(
                workService.getWorkDetail(
                        1L,
                        100L
                )
        ).thenReturn(work);

        mockMvc.perform(
                get("/works/100")
                        .requestAttr(
                                "_csrf",
                                csrfToken()
                        )
        )
        .andExpect(
               status().isOk()
        )
        .andExpect(result -> {
            String html =
                    result.getResponse().getContentAsString();

            int linkStart = html.indexOf(
                    "id=\"back-to-in-progress-link\""
            );
            assertTrue(linkStart >= 0);

            int linkEnd = html.indexOf("</a>", linkStart);
            assertTrue(linkEnd >= 0);

            String backLinkHtml =
                    html.substring(linkStart, linkEnd);

            assertTrue(backLinkHtml.contains(
                    "href=\"/works/in-progress\""
            ));
            assertTrue(backLinkHtml.contains(
                    "制作中一覧へ戻る"
            ));
            assertFalse(html.contains(
                    "id=\"back-to-completed-link\""
            ));
        })
        .andExpect(
                content().string(
                        containsString(
                                "▶ 制作中一覧"
                        )
                )
        );
    }

    @Test
    void from未指定の完成作品では完成一覧を戻り先にする()
            throws Exception {

        WorkDetailResult work =
                createWork(
                        WorkStatus.COMPLETED,
                        LocalDate.of(
                                2026,
                                10,
                                5
                        ),
                        false
                );

        when(
                workService.getWorkDetail(
                        1L,
                        100L
                )
        ).thenReturn(work);

        mockMvc.perform(
                get("/works/100")
                        .requestAttr(
                                "_csrf",
                                csrfToken()
                        )
        )
        .andExpect(
                status().isOk()
        )
        .andExpect(result -> {
            String html =
                    result.getResponse().getContentAsString();

            int linkStart = html.indexOf(
                    "id=\"back-to-completed-link\""
            );
            assertTrue(linkStart >= 0);

            int linkEnd = html.indexOf("</a>", linkStart);
            assertTrue(linkEnd >= 0);

            String backLinkHtml =
                    html.substring(linkStart, linkEnd);

            assertTrue(backLinkHtml.contains(
                    "href=\"/works/completed\""
            ));
            assertTrue(backLinkHtml.contains(
                    "完成一覧へ戻る"
            ));
            assertFalse(html.contains(
                    "id=\"back-to-in-progress-link\""
            ));
        })
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
                                "value=\"2026-10-05\""
                        )
                )
        );
    }

    @Test
    void fromが制作中なら作品が完成状態でも制作中一覧を維持する()
            throws Exception {

        WorkDetailResult work =
                createWork(
                        WorkStatus.COMPLETED,
                        LocalDate.of(
                                2026,
                                10,
                                5
                        ),
                        false
                );

        when(
                workService.getWorkDetail(
                        1L,
                        100L
                )
        ).thenReturn(work);

        mockMvc.perform(
                get("/works/100")
                        .param(
                                "from",
                                "in-progress"
                        )
                        .requestAttr(
                                "_csrf",
                                csrfToken()
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(result -> {
                    String html =
                            result.getResponse().getContentAsString();

                    int linkStart = html.indexOf(
                            "id=\"back-to-in-progress-link\""
                    );
                    assertTrue(linkStart >= 0);

                    int linkEnd = html.indexOf("</a>", linkStart);
                    assertTrue(linkEnd >= 0);

                    String backLinkHtml =
                            html.substring(linkStart, linkEnd);

                    assertTrue(backLinkHtml.contains(
                            "href=\"/works/in-progress\""
                    ));
                    assertTrue(backLinkHtml.contains(
                            "制作中一覧へ戻る"
                    ));
                    assertFalse(html.contains(
                            "id=\"back-to-completed-link\""
                    ));
                })
                .andExpect(
                        content().string(
                                containsString(
                                        "▶ 制作中一覧"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "from=in-progress"
                                )
                        )
                );
    }

    @Test
    void fromが完成なら作品が制作中状態でも完成一覧を維持する()
            throws Exception {

        WorkDetailResult work =
                createWork(
                        WorkStatus.IN_PROGRESS,
                        null,
                        false
                );

        when(
                workService.getWorkDetail(
                        1L,
                        100L
                )
        ).thenReturn(work);

        mockMvc.perform(
                get("/works/100")
                        .param(
                                "from",
                                "completed"
                        )
                        .requestAttr(
                                "_csrf",
                                csrfToken()
                        )
        		)
        .andExpect(
                status().isOk()
        )
        .andExpect(result -> {
            String html =
                    result.getResponse().getContentAsString();

            int linkStart = html.indexOf(
                    "id=\"back-to-completed-link\""
            );
            assertTrue(linkStart >= 0);

            int linkEnd = html.indexOf("</a>", linkStart);
            assertTrue(linkEnd >= 0);

            String backLinkHtml =
                    html.substring(linkStart, linkEnd);

            assertTrue(backLinkHtml.contains(
                    "href=\"/works/completed\""
            ));
            assertTrue(backLinkHtml.contains(
                    "完成一覧へ戻る"
            ));
            assertFalse(html.contains(
                    "id=\"back-to-in-progress-link\""
            ));
        })
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
                                "from=completed"
                        )
                )
        );
    }

    @Test
    void 写真登録済みなら本人保護画像URLと削除指定を表示する()
            throws Exception {

        WorkDetailResult work =
                createWork(
                        WorkStatus.IN_PROGRESS,
                        null,
                        true
                );

        when(
                workService.getWorkDetail(
                        1L,
                        100L
                )
        ).thenReturn(work);

        mockMvc.perform(
                get("/works/100")
                        .requestAttr(
                                "_csrf",
                                csrfToken()
                        )
        )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/works/100/image"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "id=\"delete-photo\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "写真を削除する"
                                )
                        )
                );
    }

    private WorkDetailResult createWork(
            WorkStatus status,
            LocalDate completionDate,
            boolean hasPhoto) {

        return new WorkDetailResult(
                100L,
                "詳細テスト作品",
                WorkCategory.HAT,
                10,
                20,
                "",
                status,
                completionDate,
                "basic-info-token",
                hasPhoto
        );
    }

    private DefaultCsrfToken csrfToken() {

        return new DefaultCsrfToken(
                "X-CSRF-TOKEN",
                "_csrf",
                "test-csrf-token"
        );
    }
}