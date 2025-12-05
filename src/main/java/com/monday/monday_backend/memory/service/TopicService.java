package com.monday.monday_backend.memory.service;

import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.entity.TopicMemoryEntity;
import com.monday.monday_backend.memory.repo.TopicMemoryRepository;
import com.monday.monday_backend.memory.repo.TopicSessionLinkRepository;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.topic.dto.TopicMemoryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final UserRepository userRepository;
    private final TopicMemoryRepository topicMemoryRepository;
    private final TopicSessionLinkRepository topicSessionLinkRepository;

    @Transactional
    public TopicMemoryResponseDTO findOrCreateTopic(String topicId, String userId) {
        return null;
    }

    @Transactional
    public SessionMemoryResponseDTO addSessionToTopic(String topicName, String userId, SessionMemoryEntity session) {
        UserEntity user = userRepository.findByUserId(UUID.fromString(userId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find our user"));

        Optional<TopicMemoryEntity> findTopic = topicMemoryRepository.findByNameAndUser(topicName, user);
        TopicMemoryEntity foundTopic = null;
        if (findTopic.isPresent()) {
            foundTopic = findTopic.get();
        } else {
            foundTopic = new TopicMemoryEntity();
            foundTopic.setUser(user);
            foundTopic.setName(topicName);
            foundTopic.setPinned(true);
        }
        // FIXME: Add more topic session data later.
        return null;
    }
}
