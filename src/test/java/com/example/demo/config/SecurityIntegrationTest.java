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
import com.example.demo.entity.Work;
import com.example.demo.entity.WorkCategory;
import com.example.demo.entity.WorkStatus;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkRepository;
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
    private WorkRepository workRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void topPageIsPublic() throws Exception {

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(
                        model().attribute(
                                "isAuthenticated",
                                false
                        )
                );
    }

    @Test
    void topPageIsAvailableWhenAuthenticated() throws Exception {

        User user = new User(
                "home-security@example.com",
                "homesecurity",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        userRepository.saveAndFlush(user);

        try {
            KnittingUserPrincipal principal =
                    new KnittingUserPrincipal(
                            user.getId(),
                            user.getUsername(),
                            user.getPasswordHash()
                    );

            var authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            mockMvc.perform(
                    get("/")
                            .with(
                                    authentication(
                                            authenticationToken
                                    )
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(view().name("home"))
                    .andExpect(
                            model().attribute(
                                    "isAuthenticated",
                                    true
                            )
                    )
                    .andExpect(
                            model().attributeExists(
                                    "inProgressWorks"
                            )
                    );

        } finally {
            userRepository.delete(user);
            userRepository.flush();
        }
    }

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
    
    @Test
    void counterPageIsPublic() throws Exception {

        mockMvc.perform(get("/counter"))
                .andExpect(status().isOk())
                .andExpect(view().name("counter"))
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
    void yarnGuidePageIsPublic() throws Exception {

        mockMvc.perform(get("/yarn-guide"))
                .andExpect(status().isOk())
                .andExpect(view().name("yarn-guide"))
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
    void inProgressWorksPageRedirectsToLoginWhenUnauthenticated()
            throws Exception {

        mockMvc.perform(get("/works/in-progress"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/login?required")
                );
    }

    @Test
    void inProgressWorksPageIsAvailableWhenAuthenticated()
            throws Exception {

        User user = new User(
                "in-progress-security@example.com",
                "inprogresssecurity",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        userRepository.saveAndFlush(user);

        try {
            KnittingUserPrincipal principal =
                    new KnittingUserPrincipal(
                            user.getId(),
                            user.getUsername(),
                            user.getPasswordHash()
                    );

            var authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            mockMvc.perform(
                    get("/works/in-progress")
                            .with(
                                    authentication(
                                            authenticationToken
                                    )
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(
                            view().name(
                                    "works/in-progress"
                            )
                    )
                    .andExpect(
                            model().attributeExists(
                                    "worksPage"
                            )
                    )
                    .andExpect(
                            model().attributeExists(
                                    "categories"
                            )
                    )
                    .andExpect(
                            content().string(
                                    org.hamcrest.Matchers
                                            .containsString(
                                                    "制作中の作品はありません。"
                                            )
                            )
                    );

        } finally {
            userRepository.delete(user);
            userRepository.flush();
        }
    }

    @Test
    void completedWorksPageRedirectsToLoginWhenUnauthenticated()
            throws Exception {

        mockMvc.perform(get("/works/completed"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/login?required")
                );
    }

    @Test
    void completedWorksPageIsAvailableWhenAuthenticated()
            throws Exception {

        User user = new User(
                "completed-security@example.com",
                "completedsecurity",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        userRepository.saveAndFlush(user);

        try {
            KnittingUserPrincipal principal =
                    new KnittingUserPrincipal(
                            user.getId(),
                            user.getUsername(),
                            user.getPasswordHash()
                    );

            var authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            mockMvc.perform(
                    get("/works/completed")
                            .with(
                                    authentication(
                                            authenticationToken
                                    )
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(
                            view().name(
                                    "works/completed"
                            )
                    )
                    .andExpect(
                            model().attributeExists(
                                    "worksPage"
                            )
                    )
                    .andExpect(
                            model().attributeExists(
                                    "categories"
                            )
                    )
                    .andExpect(
                            content().string(
                                    org.hamcrest.Matchers
                                            .containsString(
                                                    "完成した作品はありません。"
                                            )
                            )
                    );

        } finally {
            userRepository.delete(user);
            userRepository.flush();
        }
    }

    @Test
    void workAutoSaveEndpointsReturn401WhenUnauthenticated()
            throws Exception {

        String[] endpoints = {
                "/works/100/counter",
                "/works/100/memo",
                "/works/100/status",
                "/works/100/completion-date"
        };

        for (String endpoint : endpoints) {

            mockMvc.perform(
                    post(endpoint)
                            .with(csrf())
            )
                    .andExpect(
                            status().isUnauthorized()
                    )
                    .andExpect(
                            content().string("")
                    );
        }
    }

    @Test
    void workAutoSaveEndpointsReturn403WithoutCsrfWhenAuthenticated()
            throws Exception {

        String[] endpoints = {
                "/works/100/counter",
                "/works/100/memo",
                "/works/100/status",
                "/works/100/completion-date"
        };

        for (String endpoint : endpoints) {

            mockMvc.perform(
                    post(endpoint)
                            .with(
                                    user("testuser")
                            )
            )
                    .andExpect(
                            status().isForbidden()
                    );
        }
    }

    @Test
    void workAutoSaveReturns403WhenUnauthenticatedWithInvalidCsrf()
            throws Exception {

        mockMvc.perform(
                post("/works/100/counter")
                        .with(csrf().useInvalidToken())
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanUpdateOwnWorkCounter() throws Exception {

        User owner = new User(
                "owner-security@example.com",
                "ownersecurity",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        userRepository.saveAndFlush(owner);

        Work work = new Work(
                owner,
                "owner work",
                null,
                WorkCategory.HAT,
                WorkStatus.IN_PROGRESS,
                null,
                LocalDateTime.now()
        );

        workRepository.saveAndFlush(work);

        try {
            KnittingUserPrincipal principal =
                    new KnittingUserPrincipal(
                            owner.getId(),
                            owner.getUsername(),
                            owner.getPasswordHash()
                    );

            var authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            mockMvc.perform(
                    post(
                            "/works/{id}/counter",
                            work.getId()
                    )
                            .with(csrf())
                            .with(
                                    authentication(
                                            authenticationToken
                                    )
                            )
                            .param(
                                    "counterType",
                                    "STITCH"
                            )
                            .param("value", "1")
                            .param(
                                    "expectedValue",
                                    "0"
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.counterType")
                                    .value("STITCH")
                    )
                    .andExpect(
                            jsonPath("$.value")
                                    .value(1)
                    );

            Work updated =
                    workRepository.findById(
                            work.getId()
                    ).orElseThrow();

            assertEquals(
                    1,
                    updated.getStitchCount()
            );

        } finally {
            workRepository.deleteById(
                    work.getId()
            );
            workRepository.flush();

            userRepository.deleteById(
                    owner.getId()
            );
            userRepository.flush();
        }
    }

    @Test
    void otherUserCannotUpdateOwnersWorkCounter()
            throws Exception {

        User owner = new User(
                "other-owner-security@example.com",
                "otherownersecurity",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        User otherUser = new User(
                "other-user-security@example.com",
                "otherusersecurity",
                passwordEncoder.encode("Testpass1!"),
                LocalDateTime.now()
        );

        userRepository.saveAndFlush(owner);
        userRepository.saveAndFlush(otherUser);

        Work work = new Work(
                owner,
                "owner work",
                null,
                WorkCategory.HAT,
                WorkStatus.IN_PROGRESS,
                null,
                LocalDateTime.now()
        );

        workRepository.saveAndFlush(work);

        try {
            KnittingUserPrincipal principal =
                    new KnittingUserPrincipal(
                            otherUser.getId(),
                            otherUser.getUsername(),
                            otherUser.getPasswordHash()
                    );

            var authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            mockMvc.perform(
                    post(
                            "/works/{id}/counter",
                            work.getId()
                    )
                            .with(csrf())
                            .with(
                                    authentication(
                                            authenticationToken
                                    )
                            )
                            .param(
                                    "counterType",
                                    "STITCH"
                            )
                            .param("value", "1")
                            .param(
                                    "expectedValue",
                                    "0"
                            )
            )
                    .andExpect(
                            status().isNotFound()
                    )
                    .andExpect(
                            content().string("")
                    );

            Work unchanged =
                    workRepository.findById(
                            work.getId()
                    ).orElseThrow();

            assertEquals(
                    0,
                    unchanged.getStitchCount()
            );

        } finally {
            workRepository.deleteById(
                    work.getId()
            );
            workRepository.flush();

            userRepository.deleteById(
                    otherUser.getId()
            );
            userRepository.deleteById(
                    owner.getId()
            );
            userRepository.flush();
        }
    }
}