package com.monday.monday_backend.memory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.monday.monday_backend.auth.users.UserEntity;
import jakarta.persistence.*;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents how our topic links to users and their corresponding sessions
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "topic_session_link",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_topic_session_link_topic_session",
                        columnNames = { "topic_id", "session_id" }
                )
        },
        indexes = {
                @Index(name = "idx_topic_session_link_user", columnList = "user_id"),
                @Index(name = "idx_topic_session_link_topic", columnList = "topic_id"),
                @Index(name = "idx_topic_session_link_session", columnList = "session_id")
        }
)
public class TopicSessionLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owner of the topic + session.
     * Redundant with topic.user / session.user, but makes querying cheaper and safer.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * The topic this session is linked to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private TopicMemoryEntity topic;

    /**
     * The session belonging to the topic.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionMemoryEntity session;

    /**
     * How this link was created:
     *  - EXPLICIT: user chose it
     *  - AUTO: system assigned it
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 16)
    private LinkType linkType;

    /**
     * Cosine similarity between the session embedding and the topic centroid, if computed.
     * Nullable because we create the link immediately and compute similarity async.
     */
    @Column(name = "similarity_score")
    private Double similarityScore;

    /**
     * Marked true if similarity_score is very low (outlier).
     * Used so outlier sessions don't dominate topic summaries/centroids.
     */
    @Column(name = "is_outlier", nullable = false)
    private boolean outlier = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum LinkType {
        EXPLICIT,
        AUTO
    }
}
