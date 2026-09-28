package com.example.demo.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.KnittingUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginPageIsPublic() throws Exception {

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }
    
    @Test
    void protectedPageRedirectsToLoginWhenUnauthenticated() throws Exception {

        mockMvc.perform(get("/works/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?required"));
    }
    
    @Test
    void getLogoutReturns403() throws Exception {

        mockMvc.perform(get("/logout"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void postLogoutWithoutCsrfReturns403() throws Exception {

        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void postLogoutWithCsrfSucceeds() throws Exception {

        mockMvc.perform(post("/logout")
                .with(user("testuser"))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
    
    @Test
    void loginFailureRedirectsToLoginError() throws Exception {

        mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "missinguser")
                .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
    
    @Test
    void loginWithoutCsrfReturns403() throws Exception {

        mockMvc.perform(post("/login")
                .param("username", "testuser")
                .param("password", "password"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void loginSuccessRedirectsToTop() throws Exception {

        User user = new User(
                "security-login@example.com",
                "securitylogin",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        userRepository.saveAndFlush(user);

        try {
            mockMvc.perform(post("/login")
                    .with(csrf())
                    .param("username", "securitylogin")
                    .param("password", "Testpass1!"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        } finally {
            userRepository.delete(user);
            userRepository.flush();
        }
    }
    
    @Test
    void rememberMeOnCreatesCookie() throws Exception {

        User user = new User(
                "remember-on@example.com",
                "rememberon",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        userRepository.saveAndFlush(user);

        try {
            mockMvc.perform(post("/login")
                    .with(csrf())
                    .param("username", "rememberon")
                    .param("password", "Testpass1!")
                    .param("remember-me", "on"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andExpect(cookie().exists("remember-me"));
        } finally {
            userRepository.delete(user);
            userRepository.flush();
        }
    }
    
    @Test
    void rememberMeOffDoesNotCreateCookie() throws Exception {

        User user = new User(
                "remember-off@example.com",
                "rememberoff",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        userRepository.saveAndFlush(user);

        try {
            mockMvc.perform(post("/login")
                    .with(csrf())
                    .param("username", "rememberoff")
                    .param("password", "Testpass1!"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andExpect(cookie().doesNotExist("remember-me"));
        } finally {
            userRepository.delete(user);
            userRepository.flush();
        }
    }
    
    @Test
    void loginSuccessStoresSafeKnittingUserPrincipal() throws Exception {

        User user = new User(
                "principal-test@example.com",
                "principaltest",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        userRepository.saveAndFlush(user);

        try {
            var result = mockMvc.perform(post("/login")
                    .with(csrf())
                    .param("username", "principaltest")
                    .param("password", "Testpass1!"))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();

            SecurityContext securityContext =
                    (SecurityContext) result.getRequest()
                            .getSession()
                            .getAttribute(
                                    HttpSessionSecurityContextRepository
                                            .SPRING_SECURITY_CONTEXT_KEY
                            );

            KnittingUserPrincipal principal =
                    assertInstanceOf(
                            KnittingUserPrincipal.class,
                            securityContext.getAuthentication().getPrincipal()
                    );

            assertNull(principal.getPassword());

        } finally {
            userRepository.delete(user);
            userRepository.flush();
        }
    }
    
    @Test
    void authenticatedUserAccessingLoginIsRedirectedToTop() throws Exception {

        User user = new User(
                "login-page-auth@example.com",
                "loginpageauth",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        userRepository.saveAndFlush(user);

        try {
            var loginResult = mockMvc.perform(post("/login")
                    .with(csrf())
                    .param("username", "loginpageauth")
                    .param("password", "Testpass1!"))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();

            mockMvc.perform(get("/login")
                    .session(
                            (org.springframework.mock.web.MockHttpSession)
                                    loginResult.getRequest().getSession()
                    ))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));

        } finally {
            userRepository.delete(user);
            userRepository.flush();
        }
    }
    
    @Test
    void gaugeAdjustmentPageIsPublic() throws Exception {

        mockMvc.perform(get("/gauge-adjustment"))
                .andExpect(status().isOk())
                .andExpect(view().name("gauge-adjustment"))
                .andExpect(
                        content().string(
                                org.hamcrest.Matchers.containsString("ログイン")
                        )
                )
                .andExpect(
                        content().string(
                                org.hamcrest.Matchers.containsString("ユーザー登録")
                        )
                );
    }
    
    @Test
    void gaugeAdjustmentPageIsAvailableWhenAuthenticated() throws Exception {

        KnittingUserPrincipal principal =
                new KnittingUserPrincipal(
                        1L,
                        "testuser",
                        "password-hash"
                );

        var authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        mockMvc.perform(get("/gauge-adjustment")
                .with(authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(view().name("gauge-adjustment"))
                .andExpect(model().attribute("isAuthenticated", true))
                .andExpect(
                        content().string(
                                org.hamcrest.Matchers.containsString(
                                        "id=\"global-menu-button\""
                                )
                        )
                );
    }
    
    @Test
    void yarnCombinationPageIsPublic() throws Exception {

        mockMvc.perform(get("/yarn-combination"))
                .andExpect(status().isOk())
                .andExpect(view().name("yarn-combination"))
                .andExpect(
                        content().string(
                                org.hamcrest.Matchers.containsString("ログイン")
                        )
                )
                .andExpect(
                        content().string(
                                org.hamcrest.Matchers.containsString("ユーザー登録")
                        )
                );
    }
}