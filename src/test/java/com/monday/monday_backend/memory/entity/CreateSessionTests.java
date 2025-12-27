package com.monday.monday_backend.memory.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.auth.principal.PrincipalResolver;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.monday_backend.service.JwksTestSupport;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.session.GuestHandle;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.dto.SessionOptionRequestDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionScope;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.recording.RecordingScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class CreateSessionTests extends JwksTestSupport {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    SessionService sessionService;

    @MockBean
    PrincipalResolver principalResolver;

    @Test
    void createsSession_usingGuestPrincipal_noAuthUser() throws Exception {
        // Client doesn’t send principalType/principalId anymore.
        // It just sends guestKey + source info.
        CreateSessionRequestDTO requestBody =
                new CreateSessionRequestDTO(
                        "guest-key-123",       // guestKey from client (browser/Discord)
                        null,                  // topicName (can be null for now)
                        SessionSource.DISCORD,
                        "1",                    // sourceConversationKey
                        RecordingScope.PRIVATE,
                        new SessionOptionRequestDTO(SessionScope.CHANNEL, 10)
                );
        // This is what we expect the PrincipalResolver to return when no auth headers exist (guest)
        PrincipalContext guestContext = PrincipalContext.builder()
                .principalId(null) // or a UUID if you care
                .principalType(PrincipalType.GUEST)
                .accessLevel(AccessLevel.GUEST)
                .plan(EffectivePlan.GUEST_FREE)
                .quota(null)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .build();

        when(principalResolver.resolve(any(), any())).thenReturn(guestContext);

        when(sessionService.createOrReuseSession(
                any(PrincipalContext.class),
                any(CreateSessionRequestDTO.class),
                anyString())
        ).thenReturn(
                new SessionMemoryResponseDTO(
                        HttpStatus.OK,
                        RecordingScope.PRIVATE,
                        "Saved Session Memory Successfully",
                        Collections.singletonList("1"),
                        null,
                        0,
                        "guest-key-123",
                        null,
                        null
                )
        );

        ArgumentCaptor<PrincipalContext> principalContextCaptor =
                ArgumentCaptor.forClass(PrincipalContext.class);
        ArgumentCaptor<CreateSessionRequestDTO> dtoCaptor =
                ArgumentCaptor.forClass(CreateSessionRequestDTO.class);
        ArgumentCaptor<String> idempotencyKeyCaptor =
                ArgumentCaptor.forClass(String.class);

        mvc.perform(post("/api/memory/session")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        // Verify we can get the guest
        verify(principalResolver).resolve(isNull(), any(GuestHandle.class));

        // Verify we routed correctly and constructed args correctly
        verify(sessionService).createOrReuseSession(
                principalContextCaptor.capture(),
                dtoCaptor.capture(),
                idempotencyKeyCaptor.capture()
        );

        Assertions.assertNotNull(
                principalContextCaptor.getValue(),
                "PrincipalContext passed to sessionService should not be null"
        );

        PrincipalContext ctx = principalContextCaptor.getValue();
        System.out.println("Captured PrincipalContext: " + ctx);
        // Assert principal was derived as guest
        Assertions.assertEquals(PrincipalType.GUEST, principalContextCaptor.getValue().getPrincipalType());

        // Assert DTO contents came from the body
        CreateSessionRequestDTO captured = dtoCaptor.getValue();
        Assertions.assertEquals("guest-key-123", captured.guestKey());
        Assertions.assertEquals(SessionSource.DISCORD, captured.source());
        Assertions.assertEquals("1", captured.sourceConversationKey());
    }

    @Test
    void createsSession_usingUserPrincipal_withAuthUser() throws Exception {
        auth(java.util.List.of(
                new SimpleGrantedAuthority("USER")
        ));

        // Client doesn’t send principalType/principalId anymore.
        // It just sends guestKey + source info.
        CreateSessionRequestDTO requestBody =
                new CreateSessionRequestDTO(
                        "guest-key-123",       // guestKey from client (browser/Discord)
                        null,                  // topicName (can be null for now)
                        SessionSource.DISCORD,
                        "1",                    // sourceConversationKey
                        RecordingScope.PRIVATE,
                        new SessionOptionRequestDTO(SessionScope.CHANNEL, 10)
                );
        // This is what we expect the PrincipalResolver to return when auth headers exist (user)
        PrincipalContext guestContext = PrincipalContext.builder()
                .principalId(null) // or a UUID if you care
                .principalType(PrincipalType.USER)
                .accessLevel(AccessLevel.USER)
                .plan(EffectivePlan.USER_FREE)
                .quota(null)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .build();

        when(principalResolver.resolve(any(AuthUser.class), any(GuestHandle.class))).thenReturn(guestContext);

        when(sessionService.createOrReuseSession(
                any(PrincipalContext.class),
                any(CreateSessionRequestDTO.class),
                anyString())
        ).thenReturn(
                new SessionMemoryResponseDTO(
                        HttpStatus.OK,
                        RecordingScope.PRIVATE,
                        "Saved Session Memory Successfully",
                        Collections.singletonList("1"),
                        null,
                        0,
                        "guest-key-123",
                        null,
                        null
                )
        );

        ArgumentCaptor<PrincipalContext> principalContextCaptor =
                ArgumentCaptor.forClass(PrincipalContext.class);
        ArgumentCaptor<CreateSessionRequestDTO> dtoCaptor =
                ArgumentCaptor.forClass(CreateSessionRequestDTO.class);
        ArgumentCaptor<String> idempotencyKeyCaptor =
                ArgumentCaptor.forClass(String.class);

        mvc.perform(post("/api/memory/session")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        // Verify we can get the guest
        verify(principalResolver).resolve(any(AuthUser.class), any(GuestHandle.class));

        // Verify we routed correctly and constructed args correctly
        verify(sessionService).createOrReuseSession(
                principalContextCaptor.capture(),
                dtoCaptor.capture(),
                idempotencyKeyCaptor.capture()
        );

        Assertions.assertNotNull(
                principalContextCaptor.getValue(),
                "PrincipalContext passed to sessionService should not be null"
        );

        PrincipalContext ctx = principalContextCaptor.getValue();
        System.out.println("Captured PrincipalContext: " + ctx);
        // Assert principal was derived as user
        Assertions.assertEquals(PrincipalType.USER, principalContextCaptor.getValue().getPrincipalType());
        Assertions.assertEquals(AccessLevel.USER, principalContextCaptor.getValue().getAccessLevel());

        // Assert DTO contents came from the body
        CreateSessionRequestDTO captured = dtoCaptor.getValue();
        Assertions.assertEquals("guest-key-123", captured.guestKey());
        Assertions.assertEquals(SessionSource.DISCORD, captured.source());
        Assertions.assertEquals("1", captured.sourceConversationKey());
    }

    private void auth(List<SimpleGrantedAuthority> simpleAuthorities) {
        var auth = new UsernamePasswordAuthenticationToken(
                /* principal: replace with whatever your app expects */
                new AuthUser("123", "user@example.com", simpleAuthorities),
                null,
                simpleAuthorities
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
