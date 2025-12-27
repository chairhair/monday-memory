package com.monday.monday_backend.config.security;

import com.monday.monday_backend.auth.filters.DevImpersonationFilter;
import com.monday.monday_backend.auth.filters.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import java.time.Duration;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("prod")
public class SecurityConfigProd {

    private final JwtAuthFilter jwtAuthFilter;
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // strength defaults to 10
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   @Value("${app.security.mode}") String mode,
                                                   JwtDecoder jwtDecoder) throws Exception {
        var converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("ROLE_");  // scopes -> SCOPE_mem.write
        converter.setAuthoritiesClaimName("role");

        Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter =
                jwt -> new JwtAuthenticationToken(jwt,
                        converter.convert(jwt),
                        jwt.getSubject());

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/error",
                                "/webhook/stripe",
                                "/auth/**"
                        ).permitAll()

                        // Guest OR user can do these:
                        .requestMatchers(
                                "/api/memory/**",
                                "/user/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/billing/**"
                        ).permitAll()

                        // Only real accounts can do billing-ish stuff:
                        .requestMatchers(
                                "/api/payments/**",
                                "/api/account/**",
                                "/api/options/**"
                        ).authenticated() // or check type in your controller

                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.jwksUri}") String jwksUri,
                          @Value("${app.security.issuer}") String issuer,
                          @Value("${app.security.audience}") String audience) {
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        OAuth2TokenValidator<Jwt> issuerv = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audv = new AudienceValidator(audience);
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerv, audv, new JwtTimestampValidator(Duration.ofMinutes(5))));
        return nimbus;
    }

    static final class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String audience;
        AudienceValidator(String audience) { this.audience = audience; }
        public OAuth2TokenValidatorResult validate(Jwt token) {
            var aud = token.getAudience();
            return (aud != null && aud.contains(audience))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","missing/invalid audience",""));
        }
    }

}
