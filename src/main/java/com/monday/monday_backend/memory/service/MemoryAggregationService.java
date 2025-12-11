package com.monday.monday_backend.memory.service;

import com.monday.monday_backend.memory.entity.MemoryChunkEntity;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.repo.MemoryChunkRepository;
import com.monday.shared.memory.session.utils.MemoryAggregationOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class MemoryAggregationService {

    private final MemoryChunkRepository memoryChunkRepository;


    public List<MemoryChunkEntity> aggregate(
            SessionMemoryEntity session,
            MemoryAggregationOptions options
    ) {
        if (options.getMode() == null) {
            throw new IllegalArgumentException("AggregationOptions.mode must not be null");
        }
        return switch (options.getMode()) {
            case RAW -> aggregateByRaw(session);

            case LAST_N -> aggregateByNumber(
                    session,
                    requirePositive(options.getMaxChunks(), "maxChunks")
            );

            case SINCE_TIME -> aggregateByTime(
                    session,
                    requireNonNull(options.getSince(), "since"),
                    options.getMaxChunks()
            );

            case DATE_RANGE -> aggregateByDateRange(
                    session,
                    requireNonNull(options.getSince(), "since"),
                    requireNonNull(options.getUntil(), "until")
            );

            case RELEVANCE -> aggregateByRelevance(
                    session,
                    requireNonNull(options.getTopicId(), "topicId")
            );
            default -> throw new IllegalArgumentException("Mode was unable to be identified");
        };
    }


    public List<MemoryChunkEntity> aggregateByRaw(SessionMemoryEntity session) {
        return memoryChunkRepository.findBySessionOrderByOccurredAtAsc(session, Pageable.unpaged());
    }

    public List<MemoryChunkEntity> aggregateByNumber(SessionMemoryEntity session, int n) {
        return memoryChunkRepository.findBySessionOrderByOccurredAtAsc(session, PageRequest.of(0, n));
    }

    public List<MemoryChunkEntity> aggregateByTime(SessionMemoryEntity session, Instant since, int n) {
        return memoryChunkRepository
                .findBySessionAndOccurredAtAfter(session, since, PageRequest.of(0, n))
                .stream()
                .filter(c -> !c.getOccurredAt().isBefore(since))
                .toList();
    }

    // =============================
    // TODO: MLP / PRODUCT Skeletons
    // =============================

    public List<MemoryChunkEntity> aggregateByRelevance(
            SessionMemoryEntity session,
            UUID topicId
    ) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Relevance-based aggregation is not yet implemented."
        );
    }

    public List<MemoryChunkEntity> smartAggregate(SessionMemoryEntity session) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Smart aggregation is not yet implemented."
        );
    }

    public List<MemoryChunkEntity> aggregateByDateRange(
            SessionMemoryEntity session,
            Instant start,
            Instant end
    ) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Date-range aggregation is not yet implemented."
        );
    }

    private int requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

}
