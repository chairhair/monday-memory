package com.monday.monday_backend.config.security;

import com.monday.monday_backend.auth.filters.DevImpersonationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("dev")
public class SecurityConfigDev {

    @Bean
    SecurityFilterChain devSecurity(HttpSecurity http, DevImpersonationFilter devImpersonationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(devImpersonationFilter, SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    DevImpersonationFilter devImpersonationFilter(@Value("${app.security.devBypass}") boolean enabled) {
        return new DevImpersonationFilter(enabled);
    }
}
