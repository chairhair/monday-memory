package com.monday.monday_backend.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public class JwksTestSupport {
    protected static WireMockServer jwksServer;
    protected static RSAKey rsaJwk;

    @DynamicPropertySource
    static void jwtProps(DynamicPropertyRegistry registry) throws Exception {
        // Generate ephemeral RSA keypair per test run
        rsaJwk = new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).keyID("test-kid").generate();

        // Start WireMock and serve JWKS
        jwksServer = new WireMockServer(0); // random port
        jwksServer.start();

        String jwksJson = new JWKSet(rsaJwk.toPublicJWK()).toJSONObject().toString();
        jwksServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get("/.well-known/jwks.json")
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jwksJson)));

        String jwksUri = "http://localhost:" + jwksServer.port() + "/.well-known/jwks.json";
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> jwksUri);
        // Optional, if you validate these:
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://test-issuer");
    }

    public static void stop() {
        if (jwksServer != null) jwksServer.stop();
    }

    public static RSAKey getSigningKey() {
        return rsaJwk;
    }
}
