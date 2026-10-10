package com.example.demo.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void workNotFoundExceptionReturns404View() throws Exception {

        mockMvc.perform(get("/test/work-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void noResourceFoundExceptionReturns404View() throws Exception {

        mockMvc.perform(get("/test/no-resource"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Controller
    static class TestController {

        @GetMapping("/test/work-not-found")
        String workNotFound() {

            throw new WorkNotFoundException();
        }
        
        @GetMapping("/test/unexpected")
        String unexpected() {

            throw new RuntimeException("test");
        }
        
        @GetMapping("/test/number/{id}")
        String number(@PathVariable Long id) {

            return "ok";
        }
        
        @GetMapping("/test/upload-too-large")
        String uploadTooLarge() {

            throw new MaxUploadSizeExceededException(
                    11 * 1024 * 1024
            );
        }

        @GetMapping("/test/no-resource")
        String noResourceFound() throws Exception {

            throw new NoResourceFoundException(
                    HttpMethod.GET,
                    "/test/no-resource",
                    "favicon.ico"
            );
        }
    }
    
    @Test
    void unexpectedExceptionReturns500View() throws Exception {

        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"));
    }
    
    @Test
    void typeMismatchReturns400View() throws Exception {

        mockMvc.perform(get("/test/number/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"));
    }
    
    @Test
    void multipart上限超過時は10MB以下の案内を返す()
            throws Exception {

        mockMvc.perform(
                get("/test/upload-too-large")
        )
                .andExpect(
                        model().attribute(
                                "errorMessage",
                                "画像は10MB以下のファイルを選択してください。"
                        )
                );
    }
}