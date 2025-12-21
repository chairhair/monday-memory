package com.monday.monday_backend.options;

import com.monday.monday_backend.auth.users.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Maches with user 1:1 regarding how they want their model to be handled as
 */
@Getter
@Entity
@Table(name = "options")
public class OptionsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    // ===== LLM CONFIG =====
    @Setter
    @Column(name = "model_name")
    private String model;

    @Setter
    @Column(name = "temperature")
    private Double temperature;

    @Setter
    @Column(name = "top_p")
    private Double topP;

    @Setter
    @Column(name = "max_tokens")
    private Integer maxTokens;

    // ===== MEMORY CONFIG =====
    @Setter
    @Column(name = "enable_memory")
    private boolean enableMemory;

    @Setter
    @Column(name = "include_topics")
    private boolean includeTopics;

    @Setter
    @Column(name = "include_sessions")
    private boolean includeSessions;

    @Setter
    @Column(name = "max_memory_chunks")
    private Integer maxMemoryChunks;

    // ===== TARGETED SELECTION =====
    @Setter
    @ElementCollection
    @CollectionTable(
            name = "options_topic_ids",
            joinColumns = @JoinColumn(name = "option_id")
    )
    @Column(name = "topic_id")
    private List<UUID> topicIds;

    @Setter
    @ElementCollection
    @CollectionTable(
            name = "options_session_ids",
            joinColumns = @JoinColumn(name = "option_id")
    )
    @Column(name = "session_id")
    private List<UUID> sessionIds;

    // ===== PRE-INJECT PAYLOAD =====
    @Setter
    @Column(columnDefinition = "TEXT")
    private String preInject;

    // ===== CONVERSATION CONTINUITY =====
    @Setter
    @Column(name = "continue_conversation")
    private boolean continueConversation;

    @Setter
    @Column(name = "conversation_id")
    private String conversationId;

    // ===== USER / OWNERSHIP =====
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

}
