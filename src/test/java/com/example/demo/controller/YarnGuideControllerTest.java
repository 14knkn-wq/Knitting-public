package com.example.demo.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.domain.YarnThicknessStandard;

@WebMvcTest(YarnGuideController.class)
@AutoConfigureMockMvc(addFilters = false)
class YarnGuideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void yarnGuidePageIsDisplayedWithAllStandards()
            throws Exception {

        mockMvc.perform(get("/yarn-guide"))
                .andExpect(status().isOk())
                .andExpect(view().name("yarn-guide"))
                .andExpect(
                        model().attribute(
                                "yarnStandards",
                                arrayContaining(
                                        YarnThicknessStandard.values()
                                )
                        )
                );
    }
}