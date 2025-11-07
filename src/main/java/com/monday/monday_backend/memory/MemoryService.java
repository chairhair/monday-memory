package com.monday.monday_backend.memory;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The purpose of this class is to ensure that when we go to perform CRUD operations on our memory, that we:
 * - Distinguish between guest and user services
 * - Distinguish the action that's being performed
 * - Provide the appropriate feedback upon receipt.
 *
 * As such, the primary service this class performs is:
 * - Managing the caching of our DB responses
 *   + This works as an interceptor for new queries coming in to ensure that request isn't being performed again.
 *   + Works also as an cache for our session and topic data (If we know this is frequent data, we can just grab it)
 * - Performing CRUD operations on Topics AND Sessions
 * - Redirecting query requests to their appropriate classes
 */
@Service
@RequiredArgsConstructor
public class MemoryService {

    /**
     * - If Username present, calls TopicService. TopicService will most relevant information based on query and will compare it to session info later.
     * - Session info is pulled up and a TFIDF is pulled to graph latest topics.
     * - Compares and returns most relevant data back the user (for our plugin, this may be a "Hey, you might want to copy and paste this!)
     */
    @Transactional
    public void retrieveQuery() {

    }

    /**
     * 	- Exactly as it implies: Grabs most recent session data, compares it to TopicService, and then pushes it into our Topic memory
     * 	- This occurs when we're about to exit chat or we just want to save. Can be implicit/explicit.
     * 	- If it doesn't make the top k, just store a small topic blurb about it (no more than 5 words of the core concepts).
     * 		+ We can say something like "Oh, I kinda remember this. Can you tell me more?"
     */
    @Transactional
    public void upsertToTopic() {

    }

    /**
     * Completely remove any information related to a given session, both on redis and on postgres
     */
    @Transactional
    public void deleteSession() {

    }

    /**
     * Completely remove any information related to a given topic, both on redis and on postgres
     */
    @Transactional
    public void deleteTopic() {

    }

}
