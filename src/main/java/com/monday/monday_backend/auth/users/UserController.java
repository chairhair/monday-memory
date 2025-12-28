package com.monday.monday_backend.auth.users;

import com.monday.monday_backend.memory.service.LimitsProperties;
import com.monday.shared.auth.dto.*;
import com.monday.shared.memory.dto.LimitsDTO;
import com.monday.shared.memory.plan.EffectivePlan;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 *
 * Manages and provides CRUD operations to our known users
 *
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final LimitsProperties limitsProperties;

    @PostMapping("/upsert")
    public UserResponseDTO upsertUser(@RequestBody UserRequestDTO userRequestDTO) {
        return userService.upsertUser(userRequestDTO);
    }

    @DeleteMapping
    public void deleteUser(@RequestParam(required = true) List<UUID> uuids) {
        userService.deleteUsers(uuids);
    }

    @PostMapping
    public List<UserResponseDTO> getUsers(@RequestBody UserSearchRequestDTO userSearchRequestDTO) {
        return userService.retrieveUsers(userSearchRequestDTO);
    }

    @PostMapping("/identity")
    public IdentityResponseDTO getIdentity(@RequestBody ExternalLoginRequestDTO externalRequestDTO) {
        return userService.identity(externalRequestDTO);
    }

    @PostMapping("/login")
    public UserResponseDTO login(@RequestBody ExternalLoginRequestDTO externalRequestDTO) {
        return userService.loginUser(externalRequestDTO);
    }

    @PostMapping("/logout")
    public UserResponseDTO logout(@RequestBody ExternalLoginRequestDTO externalLoginRequestDTO) {
        return null;
    }

    @GetMapping("/current-plan?plan={plan}")
    public LimitsDTO getLimits(@RequestParam("plan") String plan) {
        LimitsProperties.TierLimits limit = limitsProperties.forTier(EffectivePlan.valueOf(plan));
        return new LimitsDTO(limit.getMaxTopics(), limit.getMaxSessionsPerTopic(), limit.getMonthlyTokens());
    }
}
