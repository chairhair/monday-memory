package com.monday.monday_backend.memory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "topic_chunk_link",
        uniqueConstraints = @UniqueConstraint(name="uq_topic_chunk", columnNames = {"topicId","chunkId"}))
@Getter @Setter @NoArgsConstructor
public class TopicChunkLinkEntity {

    @EmbeddedId
    private Id id;

    @MapsId("topicId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topicId", referencedColumnName = "topicId")
    private TopicMemoryEntity topic;

    @MapsId("chunkId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chunkId", referencedColumnName = "id")
    private MemoryChunkEntity chunk;

    @Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Id implements Serializable {
        @Column(length = 50) private String topicId;
        @Column(length = 50) private String chunkId;
    }
}
