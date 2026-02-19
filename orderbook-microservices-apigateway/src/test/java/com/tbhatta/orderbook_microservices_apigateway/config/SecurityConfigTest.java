package com.tbhatta.orderbook_microservices_apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.StatusAssertions;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest
@Import(SecurityConfig.class)
public class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void matchEngineEndpointShouldReturn401WithoutToken() {
        webTestClient.get()
                .uri("/match-engine/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void matchEngineEndpointShouldPassAuthWithValidJwt() {
        webTestClient
                .mutateWith(mockJwt().jwt(jwt -> jwt
                        .claim("realm_access", Map.of("roles", List.of("user")))
                ))
                .get()
                .uri("/match-engine/orders")
                .exchange()
                // 404 (no service downstream yet) or 401 (auth failed) wanted here;
                // anything else means auth passed
                .expectStatus().isNotFound();
    }
}
