package com.monday.monday_backend.query.guest;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Hey, what can I give Anon?" - This class
 *
 * Primary Use Case: For non-authenticated guest queries.
 * Requirements: None
 * Typical Use: For when the GUEST is about to inject some feedback into the ChatGPT Model
 *
 * This is an on-demand retrieval request. It can:
 * - Search across both session + topic memories.
 * - Used when the FE needs to retrieve known info on a guest from their session.
 * - It can search across session + topic memories.
 *
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/query/guest")
public class QueryGuestController {
}
