package com.monday.monday_backend.auth.principal;

import com.monday.monday_backend.auth.guests.GuestEntity;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.llm.RecallScope;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.quota.QuotaSnapshot;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.recording.RecordingScope;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class PrincipalContext {

    // Core identity
    GuestEntity guest;          // for source + guestKey + anon context
    UserEntity user;            // may be null for “pure guest”, or always non-null if you prefer
    UserPlanEntity userPlan;

    UUID principalId;           // the ID you pass into MemoryService today
    PrincipalType principalType;// whatever enum you’re using now: GUEST, USER, etc.

    // Auth / roles
    AccessLevel accessLevel;    // GUEST, USER, ADMIN…

    // Plan + quotas
    EffectivePlan plan;         // GUEST_FREE, USER_FREE, USER_PRO, etc.
    QuotaSnapshot quota;        // per-plan topic/token stats

    // Optional scopes the context can take on.
    RecordingScope recordingScope;
    RecallScope recallScope;

    public boolean isPro() {
        return plan == EffectivePlan.USER_PRO;
    }

    public boolean isLinkedAccount() {
        return user != null && user.getEmail() != null;
    }
}
