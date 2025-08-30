package com.monday.monday_backend.query.guest;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Hey, what can I give Anon?" - This class
 *
 * This is an on-demand retrieval request. It can:
 * - Search across both session + topic memories.
 * - Used when the FE needs to find info on a user.
 * - It can search across session + topic memories.
 *
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/query/guest")
public class QueryGuestController {
}
