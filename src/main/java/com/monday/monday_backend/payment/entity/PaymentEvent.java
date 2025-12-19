package com.monday.monday_backend.payment.entity;

import com.monday.monday_backend.auth.users.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "payment_event")
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Setter
    @Column(nullable = false)
    private String type;

    @Setter
    @Column(name = "stripe_event_id", nullable = false, unique = true)
    private String stripeEventId;

    @Setter
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Setter
    @Lob
    @Column(name = "payload_json")
    private String payloadJson;
}
