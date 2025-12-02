package com.monday.monday_backend.auth.guests;

import com.monday.monday_backend.auth.roles.RolesEntity;
import com.monday.monday_backend.auth.roles.RolesRepository;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.communication.entity.UserExternalAccount;
import com.monday.monday_backend.communication.repo.UserExternalAccountRepository;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.session.utils.GuestSource;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final UserRepository userRepository;
    private final UserExternalAccountRepository externalAccountRepository;
    private final RolesRepository rolesRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public GuestEntity resolveGuest(String guestKey, GuestSource source) {
        String normalizedKey = guestKey.trim();

        // 1) Fast path: guest already exists
        Optional<GuestEntity> existing =
                guestRepository.findByGuestKeyAndSource(normalizedKey, source);

        if (existing.isPresent()) {
            GuestEntity guest = existing.get();
            guest.setLastSeenAt(Instant.now(clock));
            // optional: no need to flush each time, JPA will track
            return guest;
        }
        return null;
    }

    @Transactional(readOnly = true)
    public String resolveGuestId(String guestKey, GuestSource source) {
        GuestEntity guest = resolveGuest(guestKey, source);
        return guest == null ? null : guest.getGuestId().toString();
    }

    @Transactional
    public String resolveOrCreateGuestId(String guestKey, GuestSource source) {
        String guestId = resolveGuestId(guestKey, source);

        if (guestId != null) {
            return guestId;
        }

        // 2) Create a new guest

        // It must be created as an actual user prior to saving it. This is because...
        // - It allows us to immediately pinpoint the user associated with this account.
        // - We can more easily pull roles afterward
        Optional<RolesEntity> guestAccessLevel = rolesRepository.findByAccessLevel(AccessLevel.GUEST);
        if (guestAccessLevel.isEmpty()) {
            throw new RuntimeException("There's an issue with using the guest role");
        }
        Instant now = Instant.now(clock);
        UserEntity guestUser = new UserEntity();
        guestUser.setPassword(null);
        guestUser.setEmail(null);
        guestUser.addRole(guestAccessLevel.get());
        try {
            guestUser = userRepository.save(guestUser);
        } catch (DataIntegrityViolationException dVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot save latest guest user account: "+dVE);
        }
        UserExternalAccount assignUserExternalAccount = new UserExternalAccount();
        assignUserExternalAccount.setCreatedAt(now);
        assignUserExternalAccount.setExternalId(guestId);
        assignUserExternalAccount.setUser(guestUser);
        try {
            externalAccountRepository.save(assignUserExternalAccount);
        } catch (DataIntegrityViolationException dVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot make a new external user account: "+dVE);
        }

        GuestEntity newGuest = new GuestEntity();
        newGuest.setGuestKey(guestKey.trim());
        newGuest.setSource(source);
        newGuest.setUser(guestUser);
        newGuest.setCreatedAt(now);
        newGuest.setLastSeenAt(now);

        try {
            GuestEntity saved = guestRepository.saveAndFlush(newGuest);
            return saved.getGuestId().toString();
        } catch (DataIntegrityViolationException e) {
            // 3) Race condition: someone else just created it
            Optional<GuestEntity> winner =
                    guestRepository.findByGuestKeyAndSource(guestKey.trim(), source);

            if (winner.isPresent()) {
                return winner.get().getGuestId().toString();
            }

            throw e; // if it’s still not there, something else really broke
        }
    }

}
