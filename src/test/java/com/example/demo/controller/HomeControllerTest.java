package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.example.demo.dto.InProgressWorkSummary;
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.WorkService;

class HomeControllerTest {

    private WorkService workService;
    private HomeController homeController;

    @BeforeEach
    void setUp() {
        workService = mock(WorkService.class);

        homeController = new HomeController(
                workService
        );
    }

    @Test
    void 未ログインではWorkServiceを呼ばずトップ画面を表示する() {

        Model model =
                new ExtendedModelMap();

        String viewName =
                homeController.showHome(
                        null,
                        model
                );

        assertEquals(
                "home",
                viewName
        );

        assertNull(
                model.getAttribute(
                        "inProgressWorks"
                )
        );

        verifyNoInteractions(
                workService
        );
    }

    @Test
    void ログイン済みでは本人の制作中作品最新4件をModelへ追加する() {

        KnittingUserPrincipal principal =
                mock(KnittingUserPrincipal.class);

        when(principal.getUserId())
                .thenReturn(1L);

        InProgressWorkSummary work =
                new InProgressWorkSummary(
                        101L,
                        "テスト作品",
                        25,
                        40,
                        false
                );

        var worksPage =
                new PageImpl<>(
                        List.of(work),
                        PageRequest.of(0, 4),
                        1
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        null,
                        0,
                        4
                )
        ).thenReturn(worksPage);

        Model model =
                new ExtendedModelMap();

        String viewName =
                homeController.showHome(
                        principal,
                        model
                );

        assertEquals(
                "home",
                viewName
        );

        assertSame(
                worksPage,
                model.getAttribute(
                        "inProgressWorks"
                )
        );

        verify(workService)
                .getInProgressWorks(
                        1L,
                        null,
                        0,
                        4
                );
    }
}