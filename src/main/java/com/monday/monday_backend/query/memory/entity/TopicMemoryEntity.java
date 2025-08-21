package com.monday.monday_backend.query.memory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "topic_memory",
        indexes = {
                @Index(name="idx_topic_subject", columnList = "subjectId"),
                @Index(name="idx_topic_updated", columnList = "updatedAt")
        })
@Getter @Setter @NoArgsConstructor
public class TopicMemoryEntity {

    @Id @Column(length = 50)               // ULID/string
    private String topicId;

    @Column(nullable = false, length = 64)
    private String subjectId;

    @Column(nullable = false, length = 160)
    private String title;

    @Lob
    private String summaryMd;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
