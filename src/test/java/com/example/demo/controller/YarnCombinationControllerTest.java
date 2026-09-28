package com.example.demo.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.domain.YarnCombinationMode;
import com.example.demo.dto.YarnCombinationResult;
import com.example.demo.service.YarnCombinationService;

@WebMvcTest(YarnCombinationController.class)
@AutoConfigureMockMvc(addFilters = false)
class YarnCombinationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private YarnCombinationService yarnCombinationService;

    @Test
    void initialDisplayDoesNotCallService() throws Exception {

        mockMvc.perform(get("/yarn-combination"))
                .andExpect(status().isOk())
                .andExpect(view().name("yarn-combination"));

        verifyNoInteractions(yarnCombinationService);
    }

    @Test
    void validationErrorDoesNotCallService() throws Exception {

        mockMvc.perform(get("/yarn-combination")
                        .param("calculate", "true")
                        .param("mode", "DOUBLE_STRAND")
                        .param("doubleStrandWeight", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name("yarn-combination"))
                .andExpect(
                        model().attributeHasFieldErrors(
                                "yarnCombinationForm",
                                "doubleStrandLength"
                        )
                );

        verifyNoInteractions(yarnCombinationService);
    }

    @Test
    void validDoubleStrandInputCallsServiceAndAddsResult()
            throws Exception {

        YarnCombinationResult result =
                new YarnCombinationResult(
                        120,
                        "極太",
                        "9～11号（4.8～5.4mm）",
                        "8/0～10/0号（5.0～6.0mm）"
                );

        when(yarnCombinationService.calculateCombination(
                YarnCombinationMode.DOUBLE_STRAND,
                new BigDecimal("50"),
                new BigDecimal("120"),
                null,
                null,
                null,
                null
        )).thenReturn(result);

        mockMvc.perform(get("/yarn-combination")
                        .param("calculate", "true")
                        .param("mode", "DOUBLE_STRAND")
                        .param("doubleStrandWeight", "50")
                        .param("doubleStrandLength", "120"))
                .andExpect(status().isOk())
                .andExpect(view().name("yarn-combination"))
                .andExpect(
                        model().attribute(
                                "combinationResult",
                                result
                        )
                );

        verify(yarnCombinationService).calculateCombination(
                YarnCombinationMode.DOUBLE_STRAND,
                new BigDecimal("50"),
                new BigDecimal("120"),
                null,
                null,
                null,
                null
        );
    }

    @Test
    void validCombinationInputCallsServiceAndAddsResult()
            throws Exception {

        YarnCombinationResult result =
                new YarnCombinationResult(
                        200,
                        "並太",
                        "6～8号（3.9～4.5mm）",
                        "5/0～7/0号（3.0～4.0mm）"
                );

        when(yarnCombinationService.calculateCombination(
                YarnCombinationMode.COMBINATION,
                null,
                null,
                new BigDecimal("50"),
                new BigDecimal("200"),
                new BigDecimal("50"),
                new BigDecimal("200")
        )).thenReturn(result);

        mockMvc.perform(get("/yarn-combination")
                        .param("calculate", "true")
                        .param("mode", "COMBINATION")
                        .param("yarn1Weight", "50")
                        .param("yarn1Length", "200")
                        .param("yarn2Weight", "50")
                        .param("yarn2Length", "200"))
                .andExpect(status().isOk())
                .andExpect(view().name("yarn-combination"))
                .andExpect(
                        model().attribute(
                                "combinationResult",
                                result
                        )
                );

        verify(yarnCombinationService).calculateCombination(
                YarnCombinationMode.COMBINATION,
                null,
                null,
                new BigDecimal("50"),
                new BigDecimal("200"),
                new BigDecimal("50"),
                new BigDecimal("200")
        );
    }

    @Test
    void typeMismatchDoesNotCallService() throws Exception {

        mockMvc.perform(get("/yarn-combination")
                        .param("calculate", "true")
                        .param("mode", "DOUBLE_STRAND")
                        .param("doubleStrandWeight", "abc")
                        .param("doubleStrandLength", "120"))
                .andExpect(status().isOk())
                .andExpect(view().name("yarn-combination"))
                .andExpect(
                        model().attributeHasFieldErrors(
                                "yarnCombinationForm",
                                "doubleStrandWeight"
                        )
                );

        verifyNoInteractions(yarnCombinationService);
    }

    @Test
    void calculateFalseDoesNotCallServiceEvenWhenValuesExist()
            throws Exception {

        mockMvc.perform(get("/yarn-combination")
                        .param("calculate", "false")
                        .param("mode", "DOUBLE_STRAND")
                        .param("doubleStrandWeight", "50")
                        .param("doubleStrandLength", "120"))
                .andExpect(status().isOk())
                .andExpect(view().name("yarn-combination"));

        verifyNoInteractions(yarnCombinationService);
    }
}