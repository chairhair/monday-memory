package com.monday.monday_backend.service.DevBypassTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.payment.BillingService;
import com.monday.monday_backend.payment.PaymentService;
import com.monday.monday_backend.payment.dto.StartCheckoutRequestDTO;
import com.monday.monday_backend.payment.dto.StartCheckoutResponseDTO;
import com.monday.monday_backend.service.JwksTestSupport;
import com.monday.monday_backend.service.WithAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "security.dev-bypass.enabled=true")
@ActiveProfiles({"test", "dev"})
@AutoConfigureMockMvc(addFilters = true)
public class DevBypassStripeTests extends JwksTestSupport {

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

    @WithAuth
    @Test
    void canCreateUserSubscriptionCheckout() throws Exception {

        Mockito.when(paymentService.createSubscriptionCheckout(123L, "PRO_MONTHLY",
                        "https://app/success", "https://app/cancel"))
                .thenReturn(new StartCheckoutResponseDTO("https://checkout", "cs_123"));

        var req = new StartCheckoutRequestDTO("PRO_MONTHLY", "https://app/success", "https://app/cancel");
        mvc.perform(post("/v1/payments/checkout")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(paymentService).createSubscriptionCheckout(123L,"PRO_MONTHLY","https://app/success","https://app/cancel");
    }

    @Test
    void canCreateGuestSubscriptionCheckout() throws Exception {

        Mockito.when(paymentService.createSubscriptionCheckout(null, "PRO_MONTHLY",
                        "https://app/success", "https://app/cancel"))
                .thenReturn(new StartCheckoutResponseDTO("https://checkout", "cs_123"));

        var req = new StartCheckoutRequestDTO("PRO_MONTHLY", "https://app/success", "https://app/cancel");
        mvc.perform(post("/v1/payments/checkout")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(paymentService).createSubscriptionCheckout(null,"PRO_MONTHLY","https://app/success","https://app/cancel");
    }

    @WithAuth
    @Test
    void cannotCreateUserSubscriptionCheckoutWithoutRequestBody() throws Exception {
        mvc.perform(post("/v1/payments/checkout"))
                .andExpect(status().is(500));
    }

    @Test
    void cannotCreateGuestSubscriptionCheckoutWithoutRequestBody() throws Exception {
        mvc.perform(post("/v1/payments/checkout"))
                .andExpect(status().is(500));
    }

}
