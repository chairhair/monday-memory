package com.monday.monday_backend.memory.entity;

import com.monday.monday_backend.auth.users.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "topic",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_topic_user_name",
                        columnNames = { "user_id", "name" }
                )
        },
        indexes = {
                @Index(name = "idx_topic_user", columnList = "user_id"),
                @Index(name = "idx_topic_status", columnList = "status"),
                @Index(name = "idx_topic_last_used_at", columnList = "last_used_at")
        }
)
public class TopicMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    /**
     * Owner of this topic.
     */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * User-facing name of the topic.
     * Unique per user.
     */
    @Setter
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Optional user-provided description / notes about the topic.
     */
    @Setter
    @Column(name = "description", length = 2000)
    private String description;

    /**
     * System-generated summary used for recall.
     * This is what you inject into LLM context instead of raw walls of text.
     */
    @Setter
    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    /**
     * Whether this topic is currently active or archived.
     * Archived topics won't show up in default recall, but we keep them around.
     */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TopicStatus status = TopicStatus.ACTIVE;

    /**
     * Whether the user explicitly pinned this topic as "always important".
     * This can override some recency decay in your scoring later.
     */
    @Setter
    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    /**
     * First time this topic was used / attached to a session.
     * Useful for time_span in your scoring formula.
     */
    @Setter
    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    /**
     * Last time this topic was touched (session attached, recalled, updated, etc.).
     * Drives recency_boost.
     */
    @Setter
    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    /**
     * Standard audit timestamps.
     */
    @Setter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Setter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Optional backreference to links.
     * Not required for functionality, but useful if you ever want to navigate
     * Topic → Links in code. Mark LAZY and don't overuse it.
     */
    @OneToMany(mappedBy = "topic", fetch = FetchType.LAZY)
    private Set<TopicSessionLinkEntity> sessionLinks = new HashSet<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.firstSeenAt == null) {
            this.firstSeenAt = now;
        }
        if (this.lastUsedAt == null) {
            this.lastUsedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void touchUsage() {
        this.lastUsedAt = Instant.now();
    }

    public enum TopicStatus {
        ACTIVE,
        ARCHIVED
    }
}
