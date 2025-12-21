package com.monday.monday_backend.memory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "idempotency")
public class IdempotencyEntry {
    @Id
    @Column(name = "idempotency_key", length = 150, nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "response_json", columnDefinition = "text")
    private String responseJson;

    // keep your timestamp precision/type if you need it
    @Column(name = "created_at", columnDefinition = "timestamp(6) with time zone", nullable = false)
    private java.time.Instant createdAt;

    @Column(name = "expires_at", columnDefinition = "timestamp(6) with time zone")
    private java.time.Instant expiresAt;
}
