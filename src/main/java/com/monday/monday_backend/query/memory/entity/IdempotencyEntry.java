package com.monday.monday_backend.query.memory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "idempotency")
@Getter @Setter @NoArgsConstructor
public class IdempotencyEntry {

    @Id @Column(length = 150)              // "sessionId:key" or similar
    private String key;

    @Column(length = 64, nullable = false)
    private String requestHash;

    @Lob @Basic(fetch = FetchType.LAZY)
    private String responseJson;

    private int statusCode;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;
}
