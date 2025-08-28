package com.monday.monday_backend.service.JwtAuthTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.query.memory.dto.SessionMemoryFilterRequestDTO;
import com.monday.monday_backend.query.memory.dto.SessionMemoryResponseDTO;
import com.monday.monday_backend.service.AbstractJwtResourceServerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false",
        "app.security.jwt.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles({"dev","test"})
class JwtAuthTests extends AbstractJwtResourceServerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void validJwt_allowsAccess() throws Exception {
        // Mint a token whose iss/aud matches what the app expects
        String token = JWKS.issueJwt(
                "https://auth.monday",
                "guest-audience",
                Map.of("role", "EATER", "scope", "menu:read"), // extra claims if your app uses them
                300 // 5 minutes
        );

        SessionMemoryFilterRequestDTO dto = new SessionMemoryFilterRequestDTO(null, null, null, null);

        mvc.perform(post("/v1/memory/session")
                        .header("Authorization", "Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                    )
                .andExpect(status().isOk());
    }

}
