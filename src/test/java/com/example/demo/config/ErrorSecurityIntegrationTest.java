package com.example.demo.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class ErrorSecurityIntegrationTest {

    @Autowired
    private RestTestClient restClient;
    
    @Test
    void getLogoutUsesCustom403ErrorPage() {

    	restClient.get()
                .uri("/logout")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(String.class)
                .consumeWith(result -> {

                    String body = result.getResponseBody();

                    assertNotNull(body);
                    assertTrue(
                            body.contains(
                                    "このページを表示する権限がありません。"
                            )
                    );
                });
    }
}