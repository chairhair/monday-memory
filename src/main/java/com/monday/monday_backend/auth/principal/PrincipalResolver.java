package com.monday.monday_backend.auth.principal;

import com.monday.monday_backend.auth.credentials.UserCredentialsEntity;
import com.monday.monday_backend.auth.credentials.UserCredentialsRepository;
import com.monday.monday_backend.auth.guests.GuestEntity;
import com.monday.monday_backend.auth.guests.GuestService;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserPreferencesEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.communication.entity.UserExternalAccount;
import com.monday.monday_backend.communication.repo.UserExternalAccountRepository;
import com.monday.monday_backend.memory.service.LimitsProperties;
import com.monday.monday_backend.memory.service.QuotaService;
import com.monday.monday_backend.payment.PlanDefaultsService;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.auth.utils.ExternalProvider;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.quota.QuotaSnapshot;
import com.monday.shared.memory.session.GuestHandle;
import com.monday.shared.memory.session.utils.GuestSource;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionScope;
import com.monday.shared.recording.RecordingScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Merges User and Guest classes together to make a legitimate distinction
 * between which has persistent memory.
 * Guests = No persistent memory
 * Users-FREE = Persistent memory with heavy limitations
 * Users-PRO = Persistent memory with paid features
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrincipalResolver {
    private final GuestService guestService;
    private final UserRepository userRepository;
    private final UserExternalAccountRepository userExternalAccountRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final PlanDefaultsService planDefaultsService;
    private final AccessLevelResolver accessLevelResolver;
    private final QuotaService quotaService;
    private final LimitsProperties limits;


    /**
     * Chooses what to resolve to dependent on if we're a GUEST, USER, etc.
     */
    public PrincipalContext resolve(AuthUser authUser, GuestHandle guestHandle) {
        if (authUser == null) {
            return fromGuest(guestHandle.guestKey(), guestHandle.guestSource());
        }
        return fromAuthUser(authUser, guestHandle.guestSource());
    }

    /**
     * For Discord / anonymous flows – this is what QueryGuestController should use
     */
    public PrincipalContext fromGuest(String guestKey, GuestSource source) {
        GuestEntity guest = guestService.getOrCreateGuest(guestKey, source);
        UserEntity user = guest.getUser(); // depending on your GuestService, this may always be non-null


        UserCredentialsEntity userCredentials = userCredentialsRepository.findByUser_UserId(user.getUserId()).orElse(null);

        UserPreferencesEntity userPreferences = new UserPreferencesEntity();
        userPreferences.setUser(user);
        userPreferences.setCommScope(RecordingScope.PRIVATE);
        userPreferences.setScope(SessionScope.CHANNEL);
        userPreferences.setMaxTokensPerSession(limits.getGuest().getMonthlyTokens());
        userPreferences.setMaxChunksPerSession(10L);
        user.setUserPreferences(userPreferences);

        Optional<ExternalProvider> currentProvider = ExternalProvider.fromString(source.name());
        if (currentProvider.isPresent()) {
            UserExternalAccount userExternalAccount = new UserExternalAccount();
            userExternalAccount.setExternalId(guestKey);
            userExternalAccount.setProvider(currentProvider.get());
            userExternalAccount.setCreatedAt(Instant.now());
            userExternalAccount.setUser(user);
            userExternalAccountRepository.save(userExternalAccount);
        } else {
            log.warn("Could not save the external user account to it's linker repo");
        }

        PrincipalType principalType;
        UUID principalId;

        if (userCredentials != null && user.getRoles().stream().anyMatch(x -> x.getAccessLevel().equals(AccessLevel.ADMIN) || x.getAccessLevel().equals(AccessLevel.USER))) {
            principalType = PrincipalType.USER;
            principalId = user.getUserId();
        } else {
            principalType = PrincipalType.GUEST;
            principalId = guest.getGuestId();
        }

        AccessLevel accessLevel = userCredentials != null
                ? accessLevelResolver.resolve(user.getRoles())
                : AccessLevel.GUEST;

        UserPlanEntity userPlan = resolveUserPlan(user);

        EffectivePlan effectivePlan = determineEffectivePlan(user, userPlan, userCredentials);
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
        UserCredentialsEntity userCredentials = userCredentialsRepository.findByUser_UserId(user.getUserId()).orElse(null);

        // You can either:
        //  - resolve a canonical guest per user+source, OR
        //  - allow guest to be null here.
        GuestEntity guest = guestService.getOrCreateGuestForUser(user, guestSource);


        PrincipalType principalType = PrincipalType.USER;
        UUID principalId = user.getUserId();

        AccessLevel accessLevel = accessLevelResolver.resolve(user.getRoles());

        UserPlanEntity userPlan = resolveUserPlan(user);
        EffectivePlan effectivePlan = determineEffectivePlan(user, userPlan, userCredentials);
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
                                                UserCredentialsEntity userCreds) {
        if (user == null || userCreds == null) {
            return EffectivePlan.GUEST_FREE;
        }

        // This is where you map your concrete price plan / flags to "Pro" vs "Free"
        if (userPlan != null && planDefaultsService.isProPlan(userPlan)) {
            return EffectivePlan.USER_PRO;
        }

        return EffectivePlan.USER_FREE;
    }

}
