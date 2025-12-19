package com.monday.monday_backend.analytics;

import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.shared.analytics.AnalyticsEventName;
import com.monday.shared.memory.session.utils.PrincipalType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
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
    @Column(name = "principal_key")
    private String principalKey;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type")
    private PrincipalType principalType;

    @Setter
    @Column(name = "session_id")
    private String sessionId;

    @Setter
    @Column(name = "http_result")
    private Integer httpResult;

    @Setter
    @Column(name = "error_code")
    private String errorCode;

    @Setter
    @Column(name = "latency_ms")
    private Long latencyMs;

    @Setter
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Setter
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
