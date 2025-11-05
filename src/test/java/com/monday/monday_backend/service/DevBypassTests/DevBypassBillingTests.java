package com.monday.monday_backend.service.DevBypassTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.payment.BillingService;
import com.monday.monday_backend.payment.PaymentService;
import com.monday.monday_backend.service.JwksTestSupport;
import com.monday.monday_backend.service.WithAuth;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "security.dev-bypass.enabled=true")
@ActiveProfiles({"test", "dev"})
@AutoConfigureMockMvc(addFilters = true)
public class DevBypassBillingTests extends JwksTestSupport {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    PaymentService paymentService;

    @MockitoBean
    BillingService billingService;

    @BeforeEach
    void auth(org.junit.jupiter.api.TestInfo info) {
        boolean enabled =
                info.getTestMethod().map(m -> m.isAnnotationPresent(WithAuth.class)).orElse(false)
                        || info.getTestClass().map(c -> c.isAnnotationPresent(WithAuth.class)).orElse(false);

        if (!enabled) return;
        var auth = new UsernamePasswordAuthenticationToken(
                /* principal: replace with whatever your app expects */
                new AuthUser("123", "user@example.com", java.util.List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_PRO")
                )),
                null,
                java.util.List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_PRO")
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
