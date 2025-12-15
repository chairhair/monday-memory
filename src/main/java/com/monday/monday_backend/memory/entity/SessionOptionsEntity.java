package com.monday.monday_backend.memory.entity;

import com.monday.shared.memory.session.utils.SessionScope;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "session_options")
public class SessionOptionsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "session_scope", nullable = false)
    private SessionScope scope;

    @Setter
    @Column(name = "max_chunks_per_session")
    private Integer maxChunksPerSession;

    @Setter
    @OneToOne(mappedBy = "options", fetch = FetchType.LAZY)
    private SessionMemoryEntity session;
}
