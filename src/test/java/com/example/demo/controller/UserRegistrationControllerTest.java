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
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;

import com.example.demo.exception.RegistrationValidationException;
import com.example.demo.form.UserRegistrationForm;
import com.example.demo.security.KnittingUserPrincipal;
import com.example.demo.service.UserRegistrationService;

@WebMvcTest(UserRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @Test
    void getRegisterReturnsRegistrationForm() throws Exception {

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists(
                        "userRegistrationForm"));
    }
    
    @Test
    void postRegisterWithValidInputRedirectsToLogin() throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userRegistrationService).registerUser(
                "test@example.com",
                "testuser",
                "TestPass#2026");
    }
    
    @Test
    void postRegisterWithInvalidEmailReturnsRegistrationForm() throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(
                        "userRegistrationForm",
                        "email"));

        verifyNoInteractions(userRegistrationService);
    }
    
    @Test
    void postRegisterWithValidationErrorClearsPasswords() throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                        "userRegistrationForm",
                        org.hamcrest.Matchers.hasProperty(
                                "password",
                                org.hamcrest.Matchers.nullValue())))
                .andExpect(model().attribute(
                        "userRegistrationForm",
                        org.hamcrest.Matchers.hasProperty(
                                "passwordConfirmation",
                                org.hamcrest.Matchers.nullValue())));
    }
    
    @Test
    void postRegisterWithDuplicateUsernameReturnsUsernameError() throws Exception {

        doThrow(new RegistrationValidationException(
                RegistrationValidationException.Reason.USERNAME_DUPLICATE))
                .when(userRegistrationService)
                .registerUser(
                        "test@example.com",
                        "testuser",
                        "TestPass#2026");

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(
                        "userRegistrationForm",
                        "username"))
                .andExpect(model().attributeHasFieldErrorCode(
                        "userRegistrationForm",
                        "username",
                        "USERNAME_DUPLICATE"));
    }
    
    @Test
    void postRegisterWithDuplicateEmailReturnsEmailError() throws Exception {

        doThrow(new RegistrationValidationException(
                RegistrationValidationException.Reason.EMAIL_DUPLICATE))
                .when(userRegistrationService)
                .registerUser(
                        "test@example.com",
                        "testuser",
                        "TestPass#2026");

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(
                        "userRegistrationForm",
                        "email"))
                .andExpect(model().attributeHasFieldErrorCode(
                        "userRegistrationForm",
                        "email",
                        "EMAIL_DUPLICATE"));
    }
    
    @Test
    void postRegisterWithBlockedPasswordReturnsPasswordError() throws Exception {

        doThrow(new RegistrationValidationException(
                RegistrationValidationException.Reason.PASSWORD_BLOCKED))
                .when(userRegistrationService)
                .registerUser(
                        "test@example.com",
                        "testuser",
                        "TestPass#2026");

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(
                        "userRegistrationForm",
                        "password"))
                .andExpect(model().attributeHasFieldErrorCode(
                        "userRegistrationForm",
                        "password",
                        "PASSWORD_BLOCKED"));
    }
    
    @Test
    void postRegisterWithServiceErrorClearsPasswords() throws Exception {

        doThrow(new RegistrationValidationException(
                RegistrationValidationException.Reason.PASSWORD_BLOCKED))
                .when(userRegistrationService)
                .registerUser(
                        "test@example.com",
                        "testuser",
                        "TestPass#2026");

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                        "userRegistrationForm",
                        org.hamcrest.Matchers.hasProperty(
                                "password",
                                org.hamcrest.Matchers.nullValue())))
                .andExpect(model().attribute(
                        "userRegistrationForm",
                        org.hamcrest.Matchers.hasProperty(
                                "passwordConfirmation",
                                org.hamcrest.Matchers.nullValue())));
    }
    
    @Test
    void postRegisterWithServiceErrorKeepsEmailAndUsername() throws Exception {

        doThrow(new RegistrationValidationException(
                RegistrationValidationException.Reason.PASSWORD_BLOCKED))
                .when(userRegistrationService)
                .registerUser(
                        "test@example.com",
                        "testuser",
                        "TestPass#2026");

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                        "userRegistrationForm",
                        org.hamcrest.Matchers.hasProperty(
                                "email",
                                org.hamcrest.Matchers.is("test@example.com"))))
                .andExpect(model().attribute(
                        "userRegistrationForm",
                        org.hamcrest.Matchers.hasProperty(
                                "username",
                                org.hamcrest.Matchers.is("testuser"))));
    }
    
    @Test
    void postRegisterWithPasswordMismatchReturnsConfirmationError() throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "Different#2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(
                        "userRegistrationForm",
                        "passwordConfirmation"));

        verifyNoInteractions(userRegistrationService);
    }
    
    @Test
    void postRegisterNormalizesEmailBeforeCallingService() throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "  TEST@EXAMPLE.COM  ")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().is3xxRedirection());

        verify(userRegistrationService).registerUser(
                "test@example.com",
                "testuser",
                "TestPass#2026");
    }
    
    @Test
    void postRegisterTrimsUsernameBeforeCallingService() throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "  TestUser  ")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().is3xxRedirection());

        verify(userRegistrationService).registerUser(
                "test@example.com",
                "TestUser",
                "TestPass#2026");
    }
    
    @Test
    void postRegisterKeepsPasswordExactlyAsEntered() throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "testuser")
                .param("password", " Test Pass 2026 ")
                .param("passwordConfirmation", " Test Pass 2026 "))
                .andExpect(status().is3xxRedirection());

        verify(userRegistrationService).registerUser(
                "test@example.com",
                "testuser",
                " Test Pass 2026 ");
    }
    
    @Test
    void postRegisterWithEmptyPasswordConfirmationReturnsConfirmationError()
            throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "testuser")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(
                        "userRegistrationForm",
                        "passwordConfirmation"));

        verifyNoInteractions(userRegistrationService);
    }
    
    @Test
    void postRegisterWithTooLongUsernameReturnsUsernameError()
            throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "abcdefghijklmnopqrstu")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(
                        "userRegistrationForm",
                        "username"));

        verifyNoInteractions(userRegistrationService);
    }
    
    @Test
    void getRegisterWhenLoggedInRedirectsToHome() {

        KnittingUserPrincipal principal =
                mock(KnittingUserPrincipal.class);

        String result =
                new UserRegistrationController(userRegistrationService)
                        .showRegistrationForm(
                                principal,
                                new ConcurrentModel());

        org.junit.jupiter.api.Assertions.assertEquals(
                "redirect:/",
                result);
    }
    
    @Test
    void postRegisterWhenLoggedInRedirectsToHomeWithoutRegistration() {

        KnittingUserPrincipal principal =
                mock(KnittingUserPrincipal.class);

        UserRegistrationForm form =
                new UserRegistrationForm();

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(
                        form,
                        "userRegistrationForm");

        String result =
                new UserRegistrationController(userRegistrationService)
                        .registerUser(
                                form,
                                bindingResult,
                                principal);

        org.junit.jupiter.api.Assertions.assertEquals(
                "redirect:/",
                result);

        verifyNoInteractions(userRegistrationService);
    }
    
    @Test
    void postRegisterWithInvalidUsernameCharactersReturnsUsernameError()
            throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "test_user")
                .param("password", "TestPass#2026")
                .param("passwordConfirmation", "TestPass#2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(
                        "userRegistrationForm",
                        "username"));

        verifyNoInteractions(userRegistrationService);
    }
    
    @Test
    void postRegisterWithNonAsciiPasswordReturnsPasswordError()
            throws Exception {

        mockMvc.perform(post("/register")
                .param("email", "test@example.com")
                .param("username", "testuser")
                .param("password", "Testあいう123")
                .param("passwordConfirmation", "Testあいう123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors(
                        "userRegistrationForm",
                        "password"));

        verifyNoInteractions(userRegistrationService);
    }
}