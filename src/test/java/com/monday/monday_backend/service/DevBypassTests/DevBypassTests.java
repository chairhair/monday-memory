package com.monday.monday_backend.service.DevBypassTests;

import com.monday.monday_backend.payment.BillingService;
import com.monday.monday_backend.service.JwksTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class DevBypassTests extends JwksTestSupport {

    @Autowired
    MockMvc mvc;

//    @Test
//    void withoutHeaders_accessIsOpenButUnauthenticatedContext() throws Exception {
//        mvc.perform(get("/v1/memories?userId=userA"))
//                .andExpect(status().isOk());
//    }

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
