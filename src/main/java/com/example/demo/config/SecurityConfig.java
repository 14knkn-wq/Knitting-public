package com.example.demo.config;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

@Configuration
public class SecurityConfig {
	private final KnittingSecurityProperties securityProperties;
	private final UserDetailsService userDetailsService;

	public SecurityConfig(
	        KnittingSecurityProperties securityProperties,
	        UserDetailsService userDetailsService) {

	    this.securityProperties = securityProperties;
	    this.userDetailsService = userDetailsService;
	}
	
	private static final RequestMatcher S11_ASYNC_SAVE_REQUESTS =
	        RequestMatchers.anyOf(
	                PathPatternRequestMatcher.pathPattern(
	                        HttpMethod.POST,
	                        "/works/{id}/counter"
	                ),
	                PathPatternRequestMatcher.pathPattern(
	                        HttpMethod.POST,
	                        "/works/{id}/memo"
	                ),
	                PathPatternRequestMatcher.pathPattern(
	                        HttpMethod.POST,
	                        "/works/{id}/status"
	                ),
	                PathPatternRequestMatcher.pathPattern(
	                        HttpMethod.POST,
	                        "/works/{id}/completion-date"
	                )
	        );
	
	private static final RequestMatcher GET_LOGOUT_REQUEST =
	        PathPatternRequestMatcher.pathPattern(
	                HttpMethod.GET,
	                "/logout"
	        );

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
    
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>
            rememberMeCookieSameSiteCustomizer() {

        return factory -> factory.addCookieSameSiteSuppliers(
                CookieSameSiteSupplier.ofLax()
                        .whenHasName("remember-me")
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(authorize -> authorize

                .dispatcherTypeMatchers(
                        DispatcherType.ERROR,
                        DispatcherType.FORWARD
                ).permitAll()

                .requestMatchers(
                        PathRequest.toStaticResources()
                                .atCommonLocations()
                ).permitAll()

                .requestMatchers(
                        HttpMethod.GET,
                        "/",
                        "/register",
                        "/login",
                        "/gauge-adjustment",
                        "/yarn-combination",
                        "/counter",
                        "/yarn-guide"
                ).permitAll()

                .requestMatchers(
                        HttpMethod.POST,
                        "/register",
                        "/login"
                ).permitAll()

                .requestMatchers("/works/**")
                .authenticated()

                .anyRequest()
                .denyAll()
        );
        
        http.formLogin(formLogin -> formLogin
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
        );
        
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sessionFixation ->
                        sessionFixation.changeSessionId()
                )
        );
        
        http.logout(logout -> logout
                .logoutRequestMatcher(
                        PathPatternRequestMatcher.pathPattern(
                                HttpMethod.POST,
                                "/logout"
                        )
                )
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
        );
        
        http.rememberMe(rememberMe -> rememberMe
                .key(securityProperties.rememberMeKey())
                .userDetailsService(userDetailsService)
                .rememberMeParameter("remember-me")
                .rememberMeCookieName("remember-me")
                .tokenValiditySeconds(2_592_000)
                .useSecureCookie(
                        securityProperties.rememberMeSecureCookie()
                )
        );
        
        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives(
                                "default-src 'self'; "
                                + "script-src 'self'; "
                                + "style-src 'self'; "
                                + "img-src 'self' data: blob:; "
                                + "connect-src 'self'; "
                                + "font-src 'self'; "
                                + "object-src 'none'; "
                                + "base-uri 'self'; "
                                + "frame-ancestors 'none'; "
                                + "form-action 'self';"
                        )
                )
        );
        
        AuthenticationEntryPoint loginRequiredEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/login?required");
        

        AuthenticationEntryPoint asyncUnauthorizedEntryPoint =
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        
        AuthenticationEntryPoint logoutForbiddenEntryPoint =
                (request, response, authException) ->
                        response.sendError(403);
        
        AccessDeniedHandler htmlForbiddenHandler =
                (request, response, accessDeniedException) ->
                        response.sendError(403);

                        AccessDeniedHandler asyncForbiddenHandler =
                                (request, response, accessDeniedException) -> {

                                    Authentication authentication =
                                            org.springframework.security.core.context
                                                    .SecurityContextHolder
                                                    .getContext()
                                                    .getAuthentication();

                                    if (authentication == null
                                            || !authentication.isAuthenticated()
                                            || authentication
                                                    instanceof AnonymousAuthenticationToken) {

                                        response.sendError(
                                                HttpStatus.UNAUTHORIZED.value()
                                        );

                                        return;
                                    }

                                    response.sendError(
                                            HttpStatus.FORBIDDEN.value()
                                    );
                                };
                                
        http.requestCache(requestCache -> requestCache
                .requestCache(new NullRequestCache())
        );

        http.exceptionHandling(exceptionHandling -> exceptionHandling
                .defaultAuthenticationEntryPointFor(
                        asyncUnauthorizedEntryPoint,
                        S11_ASYNC_SAVE_REQUESTS
                )
                .defaultAuthenticationEntryPointFor(
                        logoutForbiddenEntryPoint,
                        GET_LOGOUT_REQUEST
                )
                .defaultAuthenticationEntryPointFor(
                        loginRequiredEntryPoint,
                        AnyRequestMatcher.INSTANCE
                )

                .defaultAccessDeniedHandlerFor(
                        asyncForbiddenHandler,
                        S11_ASYNC_SAVE_REQUESTS
                )
                .accessDeniedHandler(htmlForbiddenHandler)
        );

        return http.build();
    }
}