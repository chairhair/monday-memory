package com.monday.monday_backend.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class JwksTestSupport {
    // 1) A tiny HTTP Server that we fully control in tests
    protected static MockWebServer server;

    // 2) The current RSA key pair, as a JWK. We'll publish the PUBLIC half, keep PRIVATE half to sign
    protected static RSAKey rsaJwk;

    public void start() throws Exception {
        server = new MockWebServer();
        server.start(); // bind to a random free port; safe for parallel test runners
        rotateKey();
    }

    public void stop() throws Exception {
        if (server != null) server.shutdown();
    }

    /** The URL Spring should fetch for JWKS (this is for our fake registry */
    public String jwksUri() {
        return "http://"+server.getHostName()+":"+server.getPort()+"/.well-known/jwks.json";
    }

    /**
     * Key Rotation: Generate a new RSA Key pair
     * @return
     */
    public void rotateKey() throws Exception {
        // Generate a 2048-bit RSA keypair in memory.
        rsaJwk = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)     // this key will be used to sign/verify tokens (sig)
                .algorithm(JWSAlgorithm.RS256)// RSASSA-PKCS1-v1_5 with SHA-256 — your standard RS256
                .keyID(UUID.randomUUID().toString()) // kid = which “wax seal mold” we’re using
                .generate();

        // Build a JWKS document that contains ONLY the PUBLIC part (never publish private key!)
        String jwksJson = new JWKSet(rsaJwk.toPublicJWK()).toJSONObject().toString();

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jwksJson));

    }

    /**
     * Mint a real JWT signed by the PRIVATE half of currentKey
     * The gateway will verify it using the PUBLIC half obtained from our JWKS URL.
     *
     * @param issuer - The ISS the app expects
     * @param audience - The AUD claim your app expects (i.e. audience)
     * @param extra - Any extra claims
     * @param ttlSec -How long this token should live, in seconds
     */
    public String issueJwt(String issuer, String audience, Map<String, Object> extra, long ttlSec) throws Exception {
        Instant now = Instant.now();

        // Build the claims: who issued, for whom, when issued, when expires.
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(ttlSec)))
                .jwtID(UUID.randomUUID().toString())
                .subject("user");

        if (extra != null) extra.forEach(claims::claim);

        // Header includes alg and kid so verifier knows which public key to pick from
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaJwk.getKeyID())
                .build();

        // Glue header + claims into a SignedJWT object
        SignedJWT jwt = new SignedJWT(header, claims.build());

        // Sign the token with the PRIVATE key (this is the "secret wax seal" step).
        JWSSigner signer = new RSASSASigner((RSAPrivateKey) rsaJwk.toPrivateKey());
        jwt.sign(signer);


        return jwt.serialize();
    }
}


