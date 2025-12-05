package com.monday.monday_backend.auth.guests;

import com.monday.monday_backend.auth.roles.RolesRepository;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.payment.PlanDefaultsService;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.session.utils.GuestSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final UserRepository userRepository;
    private final UserPlanRepository userPlanRepository;
    private final RolesRepository rolesRepository; // for default role
    private final PlanDefaultsService planDefaultsService; // default free/guest plans

    @Transactional
    public GuestEntity getOrCreateGuest(String guestKey, GuestSource source) {
        return guestRepository.findByGuestKeyAndSource(guestKey, source)
                .map(this::touchGuestAndReturn)
                .orElseGet(() -> createNewGuestWithShadowUser(guestKey, source));
    }

    @Transactional
    public GuestEntity getOrCreateGuestForUser(UserEntity user, GuestSource source) {
        return guestRepository.findByUserAndSource(user, source)
                .map(this::touchGuestAndReturn)
                .orElseGet(() -> createNewGuestForExistingUser(user, source));
    }

    @Transactional
    public GuestEntity linkGuestToUser(GuestEntity guest, UserEntity realUser) {
        // If the guest was previously linked to a shadow user, you can optionally
        // merge or drop that shadow user here. For MVP: just re-point.
        guest.setUser(realUser);
        guest.setLastSeenAt(Instant.now());
        return guestRepository.save(guest);
    }

    @Transactional
    public void touchGuest(GuestEntity guest) {
        guest.setLastSeenAt(Instant.now());
        guestRepository.save(guest);
    }

    // ----------------- internal helpers -----------------

    private GuestEntity touchGuestAndReturn(GuestEntity guest) {
        guest.setLastSeenAt(Instant.now());
        return guestRepository.save(guest);
    }

    private GuestEntity createNewGuestWithShadowUser(String guestKey, GuestSource source) {
        // 1) Create a "shadow" user with default free/guest plan
        UserEntity user = createShadowUser();

        // 2) Create guest bound to that user
        GuestEntity guest = new GuestEntity();
        guest.setGuestKey(guestKey);
        guest.setSource(source);
        guest.setUser(user);

        Instant now = Instant.now();
        guest.setCreatedAt(now);
        guest.setLastSeenAt(now);

        return guestRepository.save(guest);
    }

    private GuestEntity createNewGuestForExistingUser(UserEntity user, GuestSource source) {
        GuestEntity guest = new GuestEntity();
        guest.setGuestKey(generateGuestKeyForUser(user, source));
        guest.setSource(source);
        guest.setUser(user);

        Instant now = Instant.now();
        guest.setCreatedAt(now);
        guest.setLastSeenAt(now);

        return guestRepository.save(guest);
    }

    private UserEntity createShadowUser() {
        UserEntity user = new UserEntity();

        user.setEmail(null); // not linked yet
        user.setLinked(false);

        // default role(s) – use whatever fits your role model
        var defaultRole = rolesRepository.findByAccessLevel(AccessLevel.USER)
                .orElseThrow(() -> new IllegalStateException("Default USER role not found"));
        user.addRole(defaultRole); // or setRoles(Set.of(defaultRole))

        // Plan: default guest/free plan
        UserPlanEntity userPlan = new UserPlanEntity();
        userPlan.setPlan(planDefaultsService.getDefaultGuestPlan().getPlan());
        userPlan.setUser(user);
        userPlan.setTopicsUsed(0);
        userPlan.setTokensUsed(0L);
        // periodStart/End can be set in a PlanPeriodService if you have one

        user.setUserPlan(userPlan);

        userRepository.save(user);
        userPlanRepository.save(userPlan);

        return user;
    }

    private String generateGuestKeyForUser(UserEntity user, GuestSource source) {
        // For web/extension you might derive it; for now a random UUID is fine.
        return UUID.randomUUID().toString();
    }
}
