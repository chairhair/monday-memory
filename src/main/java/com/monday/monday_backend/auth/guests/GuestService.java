package com.monday.monday_backend.auth.guests;

import com.monday.shared.memory.session.utils.GuestSource;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final Clock clock;

    @Transactional
    public String resolveOrCreateGuestId(String guestKey, GuestSource source) {
        String normalizedKey = guestKey.trim();

        // 1) Fast path: guest already exists
        Optional<GuestEntity> existing =
                guestRepository.findByGuestKeyAndSource(normalizedKey, source);

        if (existing.isPresent()) {
            GuestEntity guest = existing.get();
            guest.setLastSeenAt(Instant.now(clock));
            // optional: no need to flush each time, JPA will track
            return guest.getGuestId().toString();
        }

        // 2) Create a new guest
        GuestEntity newGuest = new GuestEntity();
        newGuest.setGuestKey(normalizedKey);
        newGuest.setSource(source);
        Instant now = Instant.now(clock);
        newGuest.setCreatedAt(now);
        newGuest.setLastSeenAt(now);

        try {
            GuestEntity saved = guestRepository.saveAndFlush(newGuest);
            return saved.getGuestId().toString();
        } catch (DataIntegrityViolationException e) {
            // 3) Race condition: someone else just created it
            Optional<GuestEntity> winner =
                    guestRepository.findByGuestKeyAndSource(normalizedKey, source);

            if (winner.isPresent()) {
                return winner.get().getGuestId().toString();
            }

            throw e; // if it’s still not there, something else really broke
        }
    }

}
