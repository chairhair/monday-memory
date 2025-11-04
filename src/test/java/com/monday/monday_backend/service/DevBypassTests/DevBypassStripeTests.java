package com.monday.monday_backend.service.DevBypassTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.payment.BillingService;
import com.monday.monday_backend.payment.PaymentService;
import com.monday.monday_backend.payment.dto.StartCheckoutRequestDTO;
import com.monday.monday_backend.payment.dto.StartCheckoutResponseDTO;
import com.monday.monday_backend.service.JwksTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class DevBypassStripeTests extends JwksTestSupport {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    PaymentService paymentService;

    @MockitoBean
    BillingService billingService;

    @Test
    @WithMockUser
    void withoutHeaders_canCreateSubscriptionCheckout() throws Exception {

        Mockito.when(paymentService.createSubscriptionCheckout(123L, "PRO_MONTHLY",
                        "https://app/success", "https://app/cancel"))
                .thenReturn(new StartCheckoutResponseDTO("https://checkout", "cs_123"));

        var req = new StartCheckoutRequestDTO("PRO_MONTHLY", "https://app/success", "https://app/cancel");
        mvc.perform(post("/v1/payments/checkout")
                        .header("X-Dev-User","123")
                        .header("X-Dev-Roles","USER,PRO")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout"))
                .andExpect(jsonPath("$.sessionId").value("cs_123"));

        verify(paymentService).createSubscriptionCheckout(123L,"PRO_MONTHLY","https://app/success","https://app/cancel");
    }

//    @Test
//    void withImpersonationHeaders_setsUserAndScopes() throws Exception {
//        mvc.perform(post("/v1/memories")
//                        .header("X-Dev-User","userA")
//                        .header("X-Dev-Scopes","mem.write mem.read")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"userId\":\"userA\",\"text\":\"dev\",\"clientToken\":\"%s\"}".formatted(UUID.randomUUID())))
//                .andExpect(status().isCreated());
//    }
}
