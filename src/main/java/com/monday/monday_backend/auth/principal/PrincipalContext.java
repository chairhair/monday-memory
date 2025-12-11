package com.monday.monday_backend.auth.principal;

import com.monday.monday_backend.auth.guests.GuestEntity;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.memory.entity.SessionOptionsEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.llm.RecallScope;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.quota.QuotaSnapshot;
import com.monday.shared.memory.session.utils.GuestSource;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.recording.RecordingScope;
import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Value
@Builder
public class PrincipalContext {

    // Core identity
    GuestEntity guest;          // for source + guestKey + anon context
    UserEntity user;            // may be null for “pure guest”, or always non-null if you prefer
    UserPlanEntity userPlan;

    UUID principalId;           // the permanent ID you pass into MemoryService today
    PrincipalType principalType;// whatever enum you’re using now: GUEST, USER, etc.

    // Auth / roles
    AccessLevel accessLevel;    // GUEST, USER, ADMIN…

    // Plan + quotas
    EffectivePlan plan;         // GUEST_FREE, USER_FREE, USER_PRO, etc.
    QuotaSnapshot quota;        // per-plan topic/token stats

    // Optional scopes the context can take on.
    RecordingScope recordingScope;
    RecallScope recallScope;

    String externalGuestKey;    // External Key associated when there is no principal Id
    GuestSource source;         // Where the conversation originated.

    public boolean isPro() {
        return plan == EffectivePlan.USER_PRO;
    }

    public boolean isLinkedAccount() {
        return user != null && user.getEmail() != null;
    }

    public boolean isUser() {
        return principalType == PrincipalType.USER;
    }

    public boolean isGuest() {
        return principalType == PrincipalType.GUEST;
    }

    public boolean hasUserId() {
        return principalId != null;
    }

    public boolean hasGuestKey() {
        return externalGuestKey != null && !externalGuestKey.isBlank();
    }

    public SessionOptions

    public void validateShape() {
        if (isUser() && principalId == null) {
            throw new IllegalStateException("USER principal must have non-null principalId");
        }
        if (isGuest() && principalId != null) {
            throw new IllegalStateException("GUEST principal must not have principalId");
        }
    }
}
