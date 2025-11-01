package com.monday.monday_backend.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "payment_event")
public class PaymentEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Setter
    @Column(nullable=false)
    String userId;

    @Setter
    @Column(nullable=false)
    String type;

    @Setter
    @Column(nullable = false, unique = true)
    String stripeEventId;

    @Setter
    @Column(nullable=false)
    Instant receivedAt;

    // raw audit for replay
    @Setter
    @Lob
    String payloadJson;
}
