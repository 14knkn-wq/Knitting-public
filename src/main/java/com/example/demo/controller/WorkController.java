package com.example.demo.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.dto.WorkDetailResult;
import com.example.demo.dto.WorkImageResult;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.exception.ImageValidationException;
import com.example.demo.exception.InvalidSubmissionTokenException;
import com.example.demo.form.WorkBasicInfoForm;
import com.example.demo.form.WorkCreateForm;
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.SubmissionTokenManager;
import com.example.demo.service.WorkService;

@Controller
@RequestMapping("/works")
public class WorkController {

    private final WorkService workService;
    private final SubmissionTokenManager submissionTokenManager;

    public WorkController(
            WorkService workService,
            SubmissionTokenManager submissionTokenManager) {

        this.workService = workService;
        this.submissionTokenManager =
                submissionTokenManager;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {

        WorkCreateForm form =
                new WorkCreateForm();

        form.setStatus(
                WorkStatus.IN_PROGRESS
        );

        form.setSubmissionToken(
                submissionTokenManager
                        .issueWorkCreateToken()
        );

        model.addAttribute(
                "workCreateForm",
                form
        );

        model.addAttribute(
                "categories",
                WorkCategory.values()
        );

        model.addAttribute(
                "workStatuses",
                WorkStatus.values()
        );

        return "works/new";
    }

    @GetMapping("/in-progress")
    public String showInProgressWorks(
            @RequestParam(
                    name = "category",
                    required = false
            )
            WorkCategory category,
            @RequestParam(
                    name = "page",
                    defaultValue = "1"
            )
            int page,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal,
            Model model,
            HttpServletResponse response) {

        if (page < 1) {
            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            return "error/400";
        }

        int pageIndex = page - 1;

        var worksPage =
                workService.getInProgressWorks(
                        principal.getUserId(),
                        category,
                        pageIndex,
                        10
                );

        if (page > 1
                && page > worksPage.getTotalPages()) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            return "error/400";
        }

        model.addAttribute(
                "worksPage",
                worksPage
        );

        model.addAttribute(
                "categories",
                WorkCategory.values()
        );

        model.addAttribute(
                "selectedCategory",
                category
        );

        return "works/in-progress";
    }

    @GetMapping("/completed")
    public String showCompletedWorks(
            @RequestParam(
                    name = "category",
                    required = false
            )
            WorkCategory category,
            @RequestParam(
                    name = "page",
                    defaultValue = "1"
            )
            int page,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal,
            Model model,
            HttpServletResponse response) {

        if (page < 1) {
            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            return "error/400";
        }

        int pageIndex = page - 1;

        var worksPage =
                workService.getCompletedWorks(
                        principal.getUserId(),
                        category,
                        pageIndex
                );

        if (page > 1
                && page > worksPage.getTotalPages()) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            return "error/400";
        }

        model.addAttribute(
                "worksPage",
                worksPage
        );

        model.addAttribute(
                "categories",
                WorkCategory.values()
        );

        model.addAttribute(
                "selectedCategory",
                category
        );

        return "works/completed";
    }

    @GetMapping("/{id}")
    public String showWorkDetail(
            @PathVariable("id")
            Long workId,
            @RequestParam(
                    name = "from",
                    required = false
            )
            String from,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal,
            Model model) {

        String validFrom =
                normalizeFrom(from);

        WorkDetailResult work =
                workService.getWorkDetail(
                        principal.getUserId(),
                        workId
                );

        WorkBasicInfoForm form =
                new WorkBasicInfoForm();

        form.setName(
                work.name()
        );

        form.setCategory(
                work.category()
        );

        form.setDeletePhoto(false);

        form.setSubmissionToken(
                work.basicInfoSubmissionToken()
        );

        model.addAttribute(
                "workBasicInfoForm",
                form
        );

        populateWorkDetailModel(
                model,
                work,
                validFrom
        );

        return "works/detail";
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public String createWork(
            @Valid
            @ModelAttribute("workCreateForm")
            WorkCreateForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal,
            Model model) {

        if (bindingResult.hasFieldErrors(
                "submissionToken")) {

            submissionTokenManager.invalidate(
                    form.getSubmissionToken()
            );

            form.setPhoto(null);

            form.setSubmissionToken(
                    submissionTokenManager
                            .issueWorkCreateToken()
            );

            model.addAttribute(
                    "categories",
                    WorkCategory.values()
            );

            model.addAttribute(
                    "workStatuses",
                    WorkStatus.values()
            );

            return "works/new";
        }

        if (bindingResult.hasErrors()) {
            form.setPhoto(null);

            model.addAttribute(
                    "categories",
                    WorkCategory.values()
            );

            model.addAttribute(
                    "workStatuses",
                    WorkStatus.values()
            );

            return "works/new";
        }

        try {
            Long workId =
                    workService.createWork(
                            principal.getUserId(),
                            form.getName(),
                            form.getPhoto(),
                            form.getCategory(),
                            form.getStatus(),
                            form.getSubmissionToken()
                    );

            return "redirect:/works/" + workId;

        } catch (ImageValidationException e) {

            String message =
                    imageValidationMessage(e);

            bindingResult.rejectValue(
                    "photo",
                    "image.invalid",
                    message
            );

            form.setPhoto(null);

            model.addAttribute(
                    "categories",
                    WorkCategory.values()
            );

            model.addAttribute(
                    "workStatuses",
                    WorkStatus.values()
            );

            return "works/new";

        } catch (InvalidSubmissionTokenException e) {

            submissionTokenManager.invalidate(
                    form.getSubmissionToken()
            );

            form.setPhoto(null);

            form.setSubmissionToken(
                    submissionTokenManager
                            .issueWorkCreateToken()
            );

            model.addAttribute(
                    "categories",
                    WorkCategory.values()
            );

            model.addAttribute(
                    "workStatuses",
                    WorkStatus.values()
            );

            return "works/new";
        }
    }

    @PostMapping(
            value = "/{id}/basic-info",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public String updateBasicInfo(
            @PathVariable("id")
            Long workId,
            @RequestParam(
                    name = "from",
                    required = false
            )
            String from,
            @Valid
            @ModelAttribute("workBasicInfoForm")
            WorkBasicInfoForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal,
            Model model) {

        String validFrom =
                normalizeFrom(from);

        if (bindingResult.hasFieldErrors(
                "submissionToken")) {

            submissionTokenManager.invalidate(
                    form.getSubmissionToken()
            );

            return redirectToWorkDetail(
                    workId,
                    validFrom
            );
        }

        if (bindingResult.hasErrors()) {

            boolean photoDeleteConflict =
                    hasSelectedPhoto(form)
                    && Boolean.TRUE.equals(
                            form.getDeletePhoto()
                    );

            form.setPhoto(null);

            if (photoDeleteConflict) {
                form.setDeletePhoto(false);
            }

            WorkDetailResult work =
                    workService
                            .getWorkDetailForRedisplay(
                                    principal.getUserId(),
                                    workId,
                                    form.getSubmissionToken()
                            );

            populateWorkDetailModel(
                    model,
                    work,
                    validFrom
            );

            return "works/detail";
        }

        try {
            workService.updateBasicInfo(
                    principal.getUserId(),
                    workId,
                    form.getName(),
                    form.getPhoto(),
                    form.getCategory(),
                    Boolean.TRUE.equals(
                            form.getDeletePhoto()
                    ),
                    form.getSubmissionToken()
            );

            return redirectToWorkDetail(
                    workId,
                    validFrom
            );

        } catch (ImageValidationException e) {

            bindingResult.rejectValue(
                    "photo",
                    "image.invalid",
                    imageValidationMessage(e)
            );

            form.setPhoto(null);

            WorkDetailResult work =
                    workService
                            .getWorkDetailForRedisplay(
                                    principal.getUserId(),
                                    workId,
                                    form.getSubmissionToken()
                            );

            populateWorkDetailModel(
                    model,
                    work,
                    validFrom
            );

            return "works/detail";

        } catch (InvalidSubmissionTokenException e) {

            submissionTokenManager.invalidate(
                    form.getSubmissionToken()
            );

            return redirectToWorkDetail(
                    workId,
                    validFrom
            );
        }
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getWorkImage(
            @PathVariable("id")
            Long workId,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal) {

        WorkImageResult result =
                workService.getWorkImage(
                        principal.getUserId(),
                        workId
                );

        return ResponseEntity
                .ok()
                .contentType(
                        MediaType.parseMediaType(
                                result.contentType()
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline"
                )
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "private, no-store"
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .header(
                        "Cross-Origin-Resource-Policy",
                        "same-origin"
                )
                .body(
                        result.resource()
                );
    }

    @PostMapping("/{id}/delete")
    public String deleteWork(
            @PathVariable("id")
            Long workId,
            @AuthenticationPrincipal
            KnittingUserPrincipal principal) {

        workService.deleteWork(
                principal.getUserId(),
                workId
        );

        return "redirect:/";
    }

    private void populateWorkDetailModel(
            Model model,
            WorkDetailResult work,
            String from) {

        model.addAttribute(
                "work",
                work
        );

        model.addAttribute(
                "categories",
                WorkCategory.values()
        );

        model.addAttribute(
                "workStatuses",
                WorkStatus.values()
        );

        model.addAttribute(
                "from",
                from
        );
    }

    private boolean hasSelectedPhoto(
            WorkBasicInfoForm form) {

        return form.getPhoto() != null
                && form.getPhoto()
                        .getOriginalFilename() != null
                && !form.getPhoto()
                        .getOriginalFilename()
                        .isEmpty();
    }

    private String normalizeFrom(
            String from) {

        if ("in-progress".equals(from)
                || "completed".equals(from)) {
            return from;
        }

        return null;
    }

    private String redirectToWorkDetail(
            Long workId,
            String from) {

        if (from == null) {
            return "redirect:/works/" + workId;
        }

        return "redirect:/works/"
                + workId
                + "?from="
                + from;
    }

    private String imageValidationMessage(
            ImageValidationException exception) {

        return switch (exception.getReason()) {
            case EMPTY_OR_UNREADABLE ->
                "画像ファイルを正常に読み込めません。別の画像を選択してください。";

            case FILE_TOO_LARGE ->
                "画像は10MB以下のファイルを選択してください。";

            case UNSUPPORTED_FORMAT ->
                "JPEGまたはPNG形式の画像を選択してください。";

            case DIMENSIONS_TOO_LARGE ->
                "画像の幅と高さは8192px以下にしてください。";

            case DECODE_FAILED ->
                "画像ファイルを正常に読み込めません。別のJPEGまたはPNG画像を選択してください。";
        };
    }
}