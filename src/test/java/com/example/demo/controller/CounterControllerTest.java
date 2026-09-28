package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CounterController.class)
@AutoConfigureMockMvc(addFilters = false)
class CounterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void counterPageIsDisplayed() throws Exception {

        mockMvc.perform(get("/counter"))
                .andExpect(status().isOk())
                .andExpect(view().name("counter"));
    }
}