package com.monday.monday_backend.service.DevBypassTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.monday_backend.service.JwksTestSupport;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class DevBypassCreateSessionTests extends JwksTestSupport {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    MemoryService memoryService;

    @Test
    void withoutHeaders_createsSession_usingGuestPrincipal() throws Exception {
        // Client doesn’t send principalType/principalId anymore.
        // It just sends guestKey + source info.
        CreateSessionRequestDTO requestBody =
                new CreateSessionRequestDTO(
                        "guest-key-123",       // guestKey from client (browser/Discord)
                        null,                  // topicName (can be null for now)
                        SessionSource.DISCORD,
                        "1"                    // sourceConversationKey
                );

        when(memoryService.upsertToSession(
                eq(PrincipalType.GUEST),
                eq("guest-key-123"),
                any(CreateSessionRequestDTO.class))
        ).thenReturn(
                new SessionMemoryResponseDTO(
                        HttpStatus.OK,
                        "Saved Session Memory Successfully",
                        Collections.singletonList("1"),
                        null,
                        0,
                        "guest-key-123",
                        null
                )
        );

        ArgumentCaptor<PrincipalType> principalTypeCaptor =
                ArgumentCaptor.forClass(PrincipalType.class);
        ArgumentCaptor<String> principalIdCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CreateSessionRequestDTO> dtoCaptor =
                ArgumentCaptor.forClass(CreateSessionRequestDTO.class);

        mvc.perform(post("/v1/memory/session")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        // Verify we routed correctly and constructed args correctly
        verify(memoryService).upsertToSession(
                principalTypeCaptor.capture(),
                principalIdCaptor.capture(),
                dtoCaptor.capture()
        );

        // Assert principal was derived as guest
        Assertions.assertEquals(PrincipalType.GUEST, principalTypeCaptor.getValue());

        // Assert DTO contents came from the body
        CreateSessionRequestDTO captured = dtoCaptor.getValue();
        Assertions.assertEquals("guest-key-123", captured.guestKey());
        Assertions.assertEquals(SessionSource.DISCORD, captured.source());
        Assertions.assertEquals("1", captured.sourceConversationKey());
    }


    @Test
    void withoutHeaders_createsSession_usingGuestPrincipalOnReturn() throws Exception {
        // request contains guestKey
        CreateSessionRequestDTO createSessionRequestDTO =
                new CreateSessionRequestDTO("guest-key-123", null, SessionSource.DISCORD, "1");

        when(memoryService.upsertToSession(
                eq(PrincipalType.GUEST),
                eq("guest-key-123"),
                any(CreateSessionRequestDTO.class))
        ).thenReturn(
                new SessionMemoryResponseDTO(
                        HttpStatus.OK,
                        "Saved Session Memory Successfully",
                        Collections.singletonList("1"),
                        null,
                        0,
                        "guest-key-123",
                        null
                )
        );

        String responseJson = mvc.perform(post("/v1/memory/session")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(createSessionRequestDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        SessionMemoryResponseDTO response =
                mapper.readValue(responseJson, SessionMemoryResponseDTO.class);

        Assertions.assertEquals(HttpStatus.OK, response.statusCode());
        Assertions.assertEquals("Saved Session Memory Successfully", response.message());
    }


}
