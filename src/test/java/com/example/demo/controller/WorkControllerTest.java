package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import com.example.demo.dto.InProgressWorkSummary;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.form.WorkCreateForm;
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.SubmissionTokenManager;
import com.example.demo.service.WorkService;

class WorkControllerTest {

    private WorkService workService;
    private SubmissionTokenManager submissionTokenManager;
    private WorkController workController;

    @BeforeEach
    void setUp() {
        workService = mock(WorkService.class);
        submissionTokenManager =
                mock(SubmissionTokenManager.class);

        workController = new WorkController(
                workService,
                submissionTokenManager
        );
    }

    @Test
    void 作品登録画面を初期値付きで表示できる() {
        when(
                submissionTokenManager
                        .issueWorkCreateToken()
        ).thenReturn("test-token");

        Model model = new ExtendedModelMap();

        String viewName =
                workController.showCreateForm(model);

        assertEquals(
                "works/new",
                viewName
        );

        Object formObject =
                model.getAttribute(
                        "workCreateForm"
                );

        assertInstanceOf(
                WorkCreateForm.class,
                formObject
        );

        WorkCreateForm form =
                (WorkCreateForm) formObject;

        assertEquals(
                WorkStatus.IN_PROGRESS,
                form.getStatus()
        );

        assertEquals(
                "test-token",
                form.getSubmissionToken()
        );

        assertArrayEquals(
                WorkCategory.values(),
                (WorkCategory[]) model.getAttribute(
                        "categories"
                )
        );

        assertArrayEquals(
                WorkStatus.values(),
                (WorkStatus[]) model.getAttribute(
                        "workStatuses"
                )
        );

        verify(submissionTokenManager)
                .issueWorkCreateToken();

        verifyNoInteractions(workService);
    }
    
    @Test
    void 入力エラー時は作品登録せず同じ画面を再表示する() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName("");
        form.setStatus(
                WorkStatus.IN_PROGRESS
        );
        form.setSubmissionToken(
                "test-token"
        );

        BindingResult bindingResult =
                new BeanPropertyBindingResult(
                        form,
                        "workCreateForm"
                );

        bindingResult.rejectValue(
                "name",
                "NotBlank",
                "作品名を入力してください。"
        );

        Model model = new ExtendedModelMap();

        String viewName =
                workController.createWork(
                        form,
                        bindingResult,
                        null,
                        model
                );

        assertEquals(
                "works/new",
                viewName
        );

        assertTrue(
                bindingResult.hasErrors()
        );

        assertArrayEquals(
                WorkCategory.values(),
                (WorkCategory[]) model.getAttribute(
                        "categories"
                )
        );

        assertArrayEquals(
                WorkStatus.values(),
                (WorkStatus[]) model.getAttribute(
                        "workStatuses"
                )
        );

        verifyNoInteractions(workService);
    }
    
    @Test
    void 正常入力では作品を登録して詳細画面へredirectする() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName("テスト作品");
        form.setCategory(
                WorkCategory.HAT
        );
        form.setStatus(
                WorkStatus.IN_PROGRESS
        );
        form.setSubmissionToken(
                "test-token"
        );

        BindingResult bindingResult =
                new BeanPropertyBindingResult(
                        form,
                        "workCreateForm"
                );

        com.example.demo.security.KnittingUserPrincipal principal =
                mock(
                        com.example.demo.security.KnittingUserPrincipal.class
                );

        when(principal.getUserId())
                .thenReturn(1L);

        when(workService.createWork(
                1L,
                "テスト作品",
                null,
                WorkCategory.HAT,
                WorkStatus.IN_PROGRESS,
                "test-token"
        )).thenReturn(100L);

        Model model =
                new ExtendedModelMap();

        String viewName =
                workController.createWork(
                        form,
                        bindingResult,
                        principal,
                        model
                );

        assertEquals(
                "redirect:/works/100",
                viewName
        );

        verify(workService)
                .createWork(
                        1L,
                        "テスト作品",
                        null,
                        WorkCategory.HAT,
                        WorkStatus.IN_PROGRESS,
                        "test-token"
                );
    }
    
    @Test
    void 画像Validationエラー時はphotoエラーとして同じ画面を再表示する() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName("画像エラー作品");
        form.setCategory(
                WorkCategory.HAT
        );
        form.setStatus(
                WorkStatus.IN_PROGRESS
        );
        form.setSubmissionToken(
                "test-token"
        );

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "large.jpg",
                        "image/jpeg",
                        new byte[] {1, 2, 3}
                );

        form.setPhoto(photo);

        BindingResult bindingResult =
                new BeanPropertyBindingResult(
                        form,
                        "workCreateForm"
                );

        com.example.demo.security.KnittingUserPrincipal principal =
                mock(
                        com.example.demo.security.KnittingUserPrincipal.class
                );

        when(principal.getUserId())
                .thenReturn(1L);

        when(workService.createWork(
                1L,
                "画像エラー作品",
                photo,
                WorkCategory.HAT,
                WorkStatus.IN_PROGRESS,
                "test-token"
        )).thenThrow(
                new com.example.demo.exception.ImageValidationException(
                        com.example.demo.exception.ImageValidationException.Reason.FILE_TOO_LARGE
                )
        );

        Model model =
                new ExtendedModelMap();

        String viewName =
                workController.createWork(
                        form,
                        bindingResult,
                        principal,
                        model
                );

        assertEquals(
                "works/new",
                viewName
        );

        assertTrue(
                bindingResult.hasFieldErrors(
                        "photo"
                )
        );

        assertEquals(
                "画像は10MB以下のファイルを選択してください。",
                bindingResult
                        .getFieldError("photo")
                        .getDefaultMessage()
        );

        assertNull(
                form.getPhoto()
        );

        assertEquals(
                "test-token",
                form.getSubmissionToken()
        );

        assertArrayEquals(
                WorkCategory.values(),
                (WorkCategory[]) model.getAttribute(
                        "categories"
                )
        );

        assertArrayEquals(
                WorkStatus.values(),
                (WorkStatus[]) model.getAttribute(
                        "workStatuses"
                )
        );

        verify(
                submissionTokenManager,
                never()
        ).issueWorkCreateToken();
    }
    
    @Test
    void 不正なsubmissionTokenでは新tokenを発行して同じ画面を再表示する() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName("tokenエラー作品");
        form.setCategory(
                WorkCategory.HAT
        );
        form.setStatus(
                WorkStatus.IN_PROGRESS
        );
        form.setSubmissionToken(
                "old-token"
        );

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "sample.jpg",
                        "image/jpeg",
                        new byte[] {1, 2, 3}
                );

        form.setPhoto(photo);

        BindingResult bindingResult =
                new BeanPropertyBindingResult(
                        form,
                        "workCreateForm"
                );

        com.example.demo.security.KnittingUserPrincipal principal =
                mock(
                        com.example.demo.security.KnittingUserPrincipal.class
                );

        when(principal.getUserId())
                .thenReturn(1L);

        when(workService.createWork(
                1L,
                "tokenエラー作品",
                photo,
                WorkCategory.HAT,
                WorkStatus.IN_PROGRESS,
                "old-token"
        )).thenThrow(
                new com.example.demo.exception.InvalidSubmissionTokenException()
        );

        when(
                submissionTokenManager
                        .issueWorkCreateToken()
        ).thenReturn("new-token");

        Model model =
                new ExtendedModelMap();

        String viewName =
                workController.createWork(
                        form,
                        bindingResult,
                        principal,
                        model
                );

        assertEquals(
                "works/new",
                viewName
        );

        assertEquals(
                "tokenエラー作品",
                form.getName()
        );

        assertEquals(
                WorkCategory.HAT,
                form.getCategory()
        );

        assertEquals(
                WorkStatus.IN_PROGRESS,
                form.getStatus()
        );

        assertNull(
                form.getPhoto()
        );

        assertEquals(
                "new-token",
                form.getSubmissionToken()
        );

        verify(submissionTokenManager)
                .invalidate("old-token");

        verify(submissionTokenManager)
                .issueWorkCreateToken();

        assertArrayEquals(
                WorkCategory.values(),
                (WorkCategory[]) model.getAttribute(
                        "categories"
                )
        );

        assertArrayEquals(
                WorkStatus.values(),
                (WorkStatus[]) model.getAttribute(
                        "workStatuses"
                )
        );
    }
    
    @Test
    void submissionToken欠落時は新tokenを発行してServiceを呼ばない() {
        WorkCreateForm form =
                new WorkCreateForm();

        form.setName("テスト作品");
        form.setCategory(
                WorkCategory.HAT
        );
        form.setStatus(
                WorkStatus.IN_PROGRESS
        );
        form.setSubmissionToken("");

        BindingResult bindingResult =
                new BeanPropertyBindingResult(
                        form,
                        "workCreateForm"
                );

        bindingResult.rejectValue(
                "submissionToken",
                "NotBlank"
        );

        when(
                submissionTokenManager
                        .issueWorkCreateToken()
        ).thenReturn("new-token");

        Model model =
                new ExtendedModelMap();

        String viewName =
                workController.createWork(
                        form,
                        bindingResult,
                        null,
                        model
                );

        assertEquals(
                "works/new",
                viewName
        );

        assertEquals(
                "テスト作品",
                form.getName()
        );

        assertEquals(
                WorkCategory.HAT,
                form.getCategory()
        );

        assertEquals(
                WorkStatus.IN_PROGRESS,
                form.getStatus()
        );

        assertNull(
                form.getPhoto()
        );

        assertEquals(
                "new-token",
                form.getSubmissionToken()
        );

        verify(submissionTokenManager)
                .invalidate("");

        verify(submissionTokenManager)
                .issueWorkCreateToken();

        verifyNoInteractions(workService);
    }
    
    @Test
    void Validationエラー時は写真を保持しない() {

        WorkCreateForm form = new WorkCreateForm();
        form.setName("テスト作品");
        form.setCategory(WorkCategory.HAT);
        form.setStatus(WorkStatus.IN_PROGRESS);
        form.setSubmissionToken("token-1");

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "test.jpg",
                        "image/jpeg",
                        new byte[] {1, 2, 3}
                );

        form.setPhoto(photo);

        BindingResult bindingResult =
                mock(BindingResult.class);

        when(
                bindingResult.hasFieldErrors(
                        "submissionToken"
                )
        ).thenReturn(false);

        when(
                bindingResult.hasErrors()
        ).thenReturn(true);

        Model model =
                new ExtendedModelMap();

        KnittingUserPrincipal principal =
                mock(KnittingUserPrincipal.class);

        WorkController workController =
                new WorkController(
                        workService,
                        submissionTokenManager
                );

        String view =
                workController.createWork(
                        form,
                        bindingResult,
                        principal,
                        model
                );
        
        assertEquals(
                "works/new",
                view
        );

        assertNull(
                form.getPhoto()
        );

        assertEquals(
                "token-1",
                form.getSubmissionToken()
        );

        verifyNoInteractions(
                workService
        );
    }

    @Test
    void 制作中一覧の1ページ目を表示できる() {

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
                        PageRequest.of(0, 10),
                        1
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        null,
                        0,
                        10
                )
        ).thenReturn(worksPage);

        Model model =
                new ExtendedModelMap();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        String viewName =
                workController.showInProgressWorks(
                        null,
                        1,
                        principal,
                        model,
                        response
                );

        assertEquals(
                "works/in-progress",
                viewName
        );

        assertEquals(
                200,
                response.getStatus()
        );

        assertSame(
                worksPage,
                model.getAttribute("worksPage")
        );

        assertArrayEquals(
                WorkCategory.values(),
                (WorkCategory[]) model.getAttribute(
                        "categories"
                )
        );

        assertNull(
                model.getAttribute(
                        "selectedCategory"
                )
        );

        verify(workService)
                .getInProgressWorks(
                        1L,
                        null,
                        0,
                        10
                );
    }

    @Test
    void 制作中一覧ではカテゴリーとページ番号をServiceへ渡せる() {

        KnittingUserPrincipal principal =
                mock(KnittingUserPrincipal.class);

        when(principal.getUserId())
                .thenReturn(1L);

        InProgressWorkSummary work =
                new InProgressWorkSummary(
                        201L,
                        "帽子作品",
                        10,
                        20,
                        true
                );

        var worksPage =
                new PageImpl<>(
                        List.of(work),
                        PageRequest.of(1, 10),
                        15
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        WorkCategory.HAT,
                        1,
                        10
                )
        ).thenReturn(worksPage);

        Model model =
                new ExtendedModelMap();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        String viewName =
                workController.showInProgressWorks(
                        WorkCategory.HAT,
                        2,
                        principal,
                        model,
                        response
                );

        assertEquals(
                "works/in-progress",
                viewName
        );

        assertEquals(
                200,
                response.getStatus()
        );

        assertSame(
                worksPage,
                model.getAttribute("worksPage")
        );

        assertEquals(
                WorkCategory.HAT,
                model.getAttribute(
                        "selectedCategory"
                )
        );

        verify(workService)
                .getInProgressWorks(
                        1L,
                        WorkCategory.HAT,
                        1,
                        10
                );
    }

    @Test
    void 制作中一覧でpageが1未満なら400としてServiceを呼ばない() {

        KnittingUserPrincipal principal =
                mock(KnittingUserPrincipal.class);

        Model model =
                new ExtendedModelMap();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        String viewName =
                workController.showInProgressWorks(
                        null,
                        0,
                        principal,
                        model,
                        response
                );

        assertEquals(
                "error/400",
                viewName
        );

        assertEquals(
                400,
                response.getStatus()
        );

        verifyNoInteractions(workService);
    }

    @Test
    void 制作中一覧で総ページ数を超えるpageなら400とする() {

        KnittingUserPrincipal principal =
                mock(KnittingUserPrincipal.class);

        when(principal.getUserId())
                .thenReturn(1L);

        var worksPage =
                new PageImpl<InProgressWorkSummary>(
                        List.of(),
                        PageRequest.of(1, 10),
                        10
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        null,
                        1,
                        10
                )
        ).thenReturn(worksPage);

        Model model =
                new ExtendedModelMap();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        String viewName =
                workController.showInProgressWorks(
                        null,
                        2,
                        principal,
                        model,
                        response
                );

        assertEquals(
                "error/400",
                viewName
        );

        assertEquals(
                400,
                response.getStatus()
        );

        verify(workService)
                .getInProgressWorks(
                        1L,
                        null,
                        1,
                        10
                );
    }

    @Test
    void 制作中作品が0件でも1ページ目なら正常表示する() {

        KnittingUserPrincipal principal =
                mock(KnittingUserPrincipal.class);

        when(principal.getUserId())
                .thenReturn(1L);

        var worksPage =
                new PageImpl<InProgressWorkSummary>(
                        List.of(),
                        PageRequest.of(0, 10),
                        0
                );

        when(
                workService.getInProgressWorks(
                        1L,
                        null,
                        0,
                        10
                )
        ).thenReturn(worksPage);

        Model model =
                new ExtendedModelMap();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        String viewName =
                workController.showInProgressWorks(
                        null,
                        1,
                        principal,
                        model,
                        response
                );

        assertEquals(
                "works/in-progress",
                viewName
        );

        assertEquals(
                200,
                response.getStatus()
        );

        assertSame(
                worksPage,
                model.getAttribute("worksPage")
        );

        verify(workService)
                .getInProgressWorks(
                        1L,
                        null,
                        0,
                        10
                );
    }
}