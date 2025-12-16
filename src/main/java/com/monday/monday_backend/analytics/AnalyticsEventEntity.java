package com.monday.monday_backend.analytics;

import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.shared.analytics.AnalyticsEventName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Table(name = "analytics_event")
public class AnalyticsEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "event_name", nullable = false)
    private AnalyticsEventName eventName;

    @Setter
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_user_preferences"))
    private UserEntity user;

    @Setter
    @Column(name = "guest_key")
    private String guestKey;

    @Setter
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
