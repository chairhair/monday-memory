package com.monday.monday_backend.auth.users;

import com.monday.shared.memory.session.utils.SessionScope;
import com.monday.shared.recording.RecordingScope;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Table(name = "user_preferences")
@Entity
public class UserPreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Setter
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_user_preferences"))
    private UserEntity user;

    /**
     * Whether we prefer for this to go out to other users or if we want the
     * session to be global
     */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "session_scope", nullable = false)
    private SessionScope scope;

    /**
     * When a session is started, determines if the user wants to be excluded from
     * the session recording or kept on.
     */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordingScope commScope;

    @Setter
    @Column(name = "max_chunks_per_session")
    private Long maxChunksPerSession;

    @Setter
    @Column(name = "max_tokens_per_session")
    private Long maxTokensPerSession;

}
