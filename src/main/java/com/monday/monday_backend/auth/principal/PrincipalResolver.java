package com.monday.monday_backend.auth.principal;

import com.monday.monday_backend.auth.guests.GuestEntity;
import com.monday.monday_backend.auth.guests.GuestService;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.memory.service.QuotaService;
import com.monday.monday_backend.payment.PlanDefaultsService;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.quota.QuotaSnapshot;
import com.monday.shared.memory.session.utils.GuestSource;
import com.monday.shared.memory.session.utils.PrincipalType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Merges User and Guest classes together to make a legitimate distinction
 * between which has persistent memory.
 * Guests = No persistent memory
 * Users-FREE = Persistent memory with heavy limitations
 * Users-PRO = Persistent memory with paid features
 */
@Service
@RequiredArgsConstructor
public class PrincipalResolver {
    private final GuestService guestService;
    private final UserRepository userRepository;
    private final PlanDefaultsService planDefaultsService;
    private final AccessLevelResolver accessLevelResolver;
    private final QuotaService quotaService;

    /**
     * For Discord / anonymous flows – this is what QueryGuestController should use
     */
    public PrincipalContext fromGuest(String guestKey, GuestSource source) {
        GuestEntity guest = guestService.getOrCreateGuest(guestKey, source);
        UserEntity user = guest.getUser(); // depending on your GuestService, this may always be non-null

        PrincipalType principalType;
        UUID principalId;

        if (user != null) {
            principalType = PrincipalType.USER;
            principalId = user.getUserId();
        } else {
            principalType = PrincipalType.GUEST;
            principalId = guest.getGuestId();
        }

        AccessLevel accessLevel = (user != null && user.getRoles() != null)
                ? accessLevelResolver.resolve(user.getRoles())
                : AccessLevel.GUEST;

        UserPlanEntity userPlan = resolveUserPlan(user);

        EffectivePlan effectivePlan = determineEffectivePlan(user, userPlan, accessLevel);
        QuotaSnapshot quotaSnapshot = quotaService.snapshotFor(user, guest, userPlan, effectivePlan);

        return PrincipalContext.builder()
                .guest(guest)
                .user(user)
                .userPlan(userPlan)
                .principalType(principalType)
                .principalId(principalId)
                .accessLevel(accessLevel)
                .plan(effectivePlan)
                .quota(quotaSnapshot)
                .build();
    }

    /**
     * For JWT-authenticated user flows (QueryUserController).
     */
    public PrincipalContext fromAuthUser(AuthUser authUser, GuestSource guestSource) {
        UserEntity user = userRepository.findByUserId(UUID.fromString(authUser.id()))
                .orElseThrow(() -> new IllegalStateException("User not found: " + authUser.id()));

        // You can either:
        //  - resolve a canonical guest per user+source, OR
        //  - allow guest to be null here.
        GuestEntity guest = guestService.getOrCreateGuestForUser(user, guestSource);

        PrincipalType principalType = PrincipalType.USER;
        UUID principalId = user.getUserId();

        AccessLevel accessLevel = accessLevelResolver.resolve(user.getRoles());

        UserPlanEntity userPlan = resolveUserPlan(user);
        EffectivePlan effectivePlan = determineEffectivePlan(user, userPlan, accessLevel);
        QuotaSnapshot quotaSnapshot = quotaService.snapshotFor(user, guest, userPlan, effectivePlan);

        return PrincipalContext.builder()
                .guest(guest)
                .user(user)
                .userPlan(userPlan)
                .plan(effectivePlan)
                .principalType(principalType)
                .principalId(principalId)
                .accessLevel(accessLevel)
                .quota(quotaSnapshot)
                .build();
    }

    public UserPlanEntity resolveUserPlan(UserEntity user) {
        if (user == null || user.getUserPlan() == null) {
            // e.g. default guest plan entity
            return planDefaultsService.getDefaultGuestPlan();
        }
        return user.getUserPlan();
    }

    public EffectivePlan determineEffectivePlan(UserEntity user,
                                                 UserPlanEntity userPlan,
                                                 AccessLevel accessLevel) {
        if (user == null) {
            return EffectivePlan.GUEST_FREE;
        }

        // This is where you map your concrete price plan / flags to "Pro" vs "Free"
        if (userPlan != null && planDefaultsService.isProPlan(userPlan)) {
            return EffectivePlan.USER_PRO;
        }

        return EffectivePlan.USER_FREE;
    }

}
