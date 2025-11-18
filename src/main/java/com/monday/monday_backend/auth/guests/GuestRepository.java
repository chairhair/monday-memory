package com.monday.monday_backend.auth.guests;

import com.monday.shared.memory.session.utils.GuestSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestRepository extends JpaRepository<GuestEntity, Long> {
    Optional<GuestEntity> findByGuestKeyAndSource(String guestKey, GuestSource source);
}
