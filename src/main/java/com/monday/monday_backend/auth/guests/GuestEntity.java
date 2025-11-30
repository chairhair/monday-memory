package com.monday.monday_backend.auth.guests;

import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.shared.memory.session.utils.GuestSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents the anonymous external identity
 */
@Getter
@Entity
@Table(
    name = "guest",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_guest_key_source",
            columnNames = { "guest_key", "source" }
        )
    }
)
public class GuestEntity {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID guestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @Setter
    private UserEntity user;

    @Getter
    @Setter
    @Column(name = "guest_key", nullable = false, length = 255)
    private String guestKey;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private GuestSource source; // DISCORD, BROWSER, etc. (tells us where the session was created)

    @Getter
    @Setter
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Getter
    @Setter
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;
}
