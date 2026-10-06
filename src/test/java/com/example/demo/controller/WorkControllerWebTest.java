package com.example.demo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
}