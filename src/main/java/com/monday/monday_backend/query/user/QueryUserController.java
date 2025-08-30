package com.monday.monday_backend.query.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Hey, what can I give my user?" - This class
 *
 * This is an on-demand retrieval request. It can:
 * - Search across both session + topic memories.
 * - Used when the FE needs to find info on a user.
 * - It can search across session + topic memories.
 *
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/query/user")
public class QueryUserController {
}
