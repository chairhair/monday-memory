package com.monday.monday_backend.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;
import java.util.Date;
import java.util.List;

public class JwtFactory {
    public static String token(String subject, List<String> roles) throws Exception {
        var now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer("http://test-issuer")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .claim("scope", String.join(" ", roles))
                .build();

        var signer = new RSASSASigner(JwksTestSupport.getSigningKey().toPrivateKey());
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-kid").build();
        var jwt = new SignedJWT(header, claims);
        jwt.sign(signer);
        return jwt.serialize();
    }
}