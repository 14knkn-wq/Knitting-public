package com.example.demo.controller;

import static org.hamcrest.Matchers.*;
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

import com.example.demo.dto.GaugeAdjustmentResult;
import com.example.demo.service.GaugeAdjustmentService;

@WebMvcTest(GaugeAdjustmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class GaugeAdjustmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GaugeAdjustmentService gaugeAdjustmentService;
    
    @Test
    void initialDisplayDoesNotCallService() throws Exception {

        mockMvc.perform(get("/gauge-adjustment"))
                .andExpect(status().isOk())
                .andExpect(view().name("gauge-adjustment"));

        verifyNoInteractions(gaugeAdjustmentService);
    }
    
    @Test
    void validationErrorDoesNotCallService() throws Exception {

        mockMvc.perform(get("/gauge-adjustment")
                        .param("specifiedStitchGauge", "20")
                        .param("measuredStitchGauge", "22"))
                .andExpect(status().isOk())
                .andExpect(view().name("gauge-adjustment"))
                .andExpect(
                        model().attributeHasFieldErrors(
                                "gaugeAdjustmentForm",
                                "originalStitchCount"
                        )
                );

        verifyNoInteractions(gaugeAdjustmentService);
    }
    
    @Test
    void validStitchInputCallsServiceAndAddsResult() throws Exception {

        GaugeAdjustmentResult result =
                new GaugeAdjustmentResult(
                        new BigDecimal("110"),
                        110,
                        null,
                        null
                );

        when(gaugeAdjustmentService.calculateAdjustment(
                new BigDecimal("20"),
                new BigDecimal("22"),
                100,
                null,
                null,
                null
        )).thenReturn(result);

        mockMvc.perform(get("/gauge-adjustment")
                        .param("specifiedStitchGauge", "20")
                        .param("measuredStitchGauge", "22")
                        .param("originalStitchCount", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("gauge-adjustment"))
                .andExpect(model().attribute("adjustmentResult", result));

        verify(gaugeAdjustmentService).calculateAdjustment(
                new BigDecimal("20"),
                new BigDecimal("22"),
                100,
                null,
                null,
                null
        );
    }
    
    @Test
    void typeMismatchDoesNotCallService() throws Exception {

        mockMvc.perform(get("/gauge-adjustment")
                        .param("specifiedStitchGauge", "20")
                        .param("measuredStitchGauge", "22")
                        .param("originalStitchCount", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("gauge-adjustment"))
                .andExpect(
                        model().attributeHasFieldErrors(
                                "gaugeAdjustmentForm",
                                "originalStitchCount"
                        )
                );

        verifyNoInteractions(gaugeAdjustmentService);
    }
    
    @Test
    void validRowInputCallsServiceAndAddsResult() throws Exception {

        GaugeAdjustmentResult result =
                new GaugeAdjustmentResult(
                        null,
                        null,
                        new BigDecimal("110"),
                        110
                );

        when(gaugeAdjustmentService.calculateAdjustment(
                null,
                null,
                null,
                new BigDecimal("30"),
                new BigDecimal("33"),
                100
        )).thenReturn(result);

        mockMvc.perform(get("/gauge-adjustment")
                        .param("specifiedRowGauge", "30")
                        .param("measuredRowGauge", "33")
                        .param("originalRowCount", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("gauge-adjustment"))
                .andExpect(model().attribute("adjustmentResult", result));

        verify(gaugeAdjustmentService).calculateAdjustment(
                null,
                null,
                null,
                new BigDecimal("30"),
                new BigDecimal("33"),
                100
        );
    }
    
    @Test
    void typeMismatchDisplaysOnlyPriorityMessage() throws Exception {

        mockMvc.perform(get("/gauge-adjustment")
                        .param("specifiedStitchGauge", "20")
                        .param("measuredStitchGauge", "22")
                        .param("originalStitchCount", "abc"))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "元の目数は1～9999の整数で入力してください。"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "元の目数を入力してください。"
                                        )
                                )
                        )
                );
    }
    
    @Test
    void validStitchAndRowInputCallsServiceAndAddsResult() throws Exception {

        GaugeAdjustmentResult result =
                new GaugeAdjustmentResult(
                        new BigDecimal("110"),
                        110,
                        new BigDecimal("120"),
                        120
                );

        when(gaugeAdjustmentService.calculateAdjustment(
                new BigDecimal("20"),
                new BigDecimal("22"),
                100,
                new BigDecimal("30"),
                new BigDecimal("36"),
                100
        )).thenReturn(result);

        mockMvc.perform(get("/gauge-adjustment")
                        .param("specifiedStitchGauge", "20")
                        .param("measuredStitchGauge", "22")
                        .param("originalStitchCount", "100")
                        .param("specifiedRowGauge", "30")
                        .param("measuredRowGauge", "36")
                        .param("originalRowCount", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("gauge-adjustment"))
                .andExpect(model().attribute("adjustmentResult", result));

        verify(gaugeAdjustmentService).calculateAdjustment(
                new BigDecimal("20"),
                new BigDecimal("22"),
                100,
                new BigDecimal("30"),
                new BigDecimal("36"),
                100
        );
    }
    
    @Test
    void displaysCalculatedValueWithMaximumFiveDecimalPlaces() throws Exception {

        GaugeAdjustmentResult result =
                new GaugeAdjustmentResult(
                        new BigDecimal("12.345678"),
                        12,
                        null,
                        null
                );

        when(gaugeAdjustmentService.calculateAdjustment(
                new BigDecimal("20"),
                new BigDecimal("22"),
                100,
                null,
                null,
                null
        )).thenReturn(result);

        mockMvc.perform(get("/gauge-adjustment")
                        .param("specifiedStitchGauge", "20")
                        .param("measuredStitchGauge", "22")
                        .param("originalStitchCount", "100"))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                containsString("12.34568")
                        )
                );
    }
    
    @Test
    void removesTrailingZerosFromCalculatedValue() throws Exception {

        GaugeAdjustmentResult result =
                new GaugeAdjustmentResult(
                        new BigDecimal("12.50000"),
                        13,
                        null,
                        null
                );

        when(gaugeAdjustmentService.calculateAdjustment(
                new BigDecimal("20"),
                new BigDecimal("22"),
                100,
                null,
                null,
                null
        )).thenReturn(result);

        mockMvc.perform(get("/gauge-adjustment")
                        .param("specifiedStitchGauge", "20")
                        .param("measuredStitchGauge", "22")
                        .param("originalStitchCount", "100"))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                containsString("12.5")
                        )
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString("12.50000")
                                )
                        )
                );
    }
    
    @Test
    void typeMismatchRetainsRejectedInputValue() throws Exception {

        mockMvc.perform(get("/gauge-adjustment")
                        .param("specifiedStitchGauge", "20")
                        .param("measuredStitchGauge", "22")
                        .param("originalStitchCount", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("gauge-adjustment"))
                .andExpect(
                        content().string(
                                containsString("value=\"abc\"")
                        )
                );

        verifyNoInteractions(gaugeAdjustmentService);
    }
    
    @Test
    void allEmptyParametersAreInitialDisplay() throws Exception {

        mockMvc.perform(get("/gauge-adjustment")
                        .param("specifiedStitchGauge", "")
                        .param("measuredStitchGauge", "")
                        .param("originalStitchCount", "")
                        .param("specifiedRowGauge", "")
                        .param("measuredRowGauge", "")
                        .param("originalRowCount", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("gauge-adjustment"));

        verifyNoInteractions(gaugeAdjustmentService);
    }
}