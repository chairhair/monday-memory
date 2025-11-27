package com.monday.monday_backend.auth.users.helper;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.shared.memory.dto.RequestMemoryChunkDTO;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.UpdateSessionRequestDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.query.guest.dto.QueryGuestRequestDTO;

/**
 * Used Specifically as a shorthand for handling AuthUser and createRequestDTO entries
 */
public record PrincipalEntry(PrincipalType principalType, String principalId) {
    private static PrincipalEntry authRetrievalHelper(AuthUser authUser, String guestKey) {
        PrincipalType principalType;
        String principalId;

        if (authUser != null) {
            principalType = PrincipalType.USER;
            principalId = authUser.id(); // or whatever your ID type is
        } else {
            principalType = PrincipalType.GUEST;
            principalId = guestKey; // or guestKey, depending on how you've modeled it
        }
        return new PrincipalEntry(principalType, principalId);
    }

    public static PrincipalEntry authRetrieval(AuthUser user, CreateSessionRequestDTO createSessionRequestDTO) {
        return authRetrievalHelper(user, createSessionRequestDTO.guestKey());
    }

    public static PrincipalEntry authRetrieval(AuthUser user, UpdateSessionRequestDTO updateSessionRequestDTO) {
        return authRetrievalHelper(user, updateSessionRequestDTO.guestKey());
    }

    public static PrincipalEntry authRetrieval(AuthUser user, RequestMemoryChunkDTO requestMemoryChunkDTO) {
        return authRetrievalHelper(user, requestMemoryChunkDTO.principalId());
    }

    public static PrincipalEntry authRetrieval(AuthUser user, QueryGuestRequestDTO requestMemoryChunkDTO) {
        return authRetrievalHelper(user, requestMemoryChunkDTO.principalId());
    }
}
