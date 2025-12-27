package com.monday.monday_backend.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
public abstract class AbstractJwtResourceServerTest {

    // 1) One support instance shared across tests in this class.
    protected static final JwksTestSupport JWKS = new JwksTestSupport();

    // 2) Start/stop the fake JWKS server once for the class.
    @BeforeAll
    static void up() throws Exception { JWKS.start(); }
    @AfterAll
    static void down() throws Exception { JWKS.stop(); }

    // 3) At context build time, tell Spring to use OUR JWKS URL.
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", JWKS::jwksUri);

        // If you followed the “property-gated” security config:
        // - enable JWT path, disable dev bypass for this test
        r.add("app.security.jwt.enabled", () -> "true");

        // Note: Add app.security.expected-issuer/expected-audience if you want to
        r.add("monday.security.expected-issuer", () -> "https://auth.monday");
        r.add("monday.security.expected-audience", () -> "guest-audience");
    }

    @Autowired
    protected MockMvc mvc;
}
