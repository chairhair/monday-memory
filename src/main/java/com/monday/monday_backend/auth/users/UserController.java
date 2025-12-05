package com.monday.monday_backend.auth.users;

import com.monday.shared.auth.dto.ExternalLoginRequestDTO;
import com.monday.shared.auth.dto.UserRequestDTO;
import com.monday.shared.auth.dto.UserResponseDTO;
import com.monday.shared.auth.dto.UserSearchRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/upsert")
    public UserResponseDTO upsertUser(@RequestBody UserRequestDTO userRequestDTO) {
        return userService.upsertUser(userRequestDTO);
    }

    @DeleteMapping
    public void deleteUser(@RequestParam(required = true) List<Long> uuids) {
        userService.deleteUsers(uuids);
    }

    @PostMapping
    public List<UserResponseDTO> getUsers(@RequestBody UserSearchRequestDTO userSearchRequestDTO) {
        return userService.retrieveUsers(userSearchRequestDTO);
    }

    @PostMapping("/login")
    public UserResponseDTO login(@RequestBody ExternalLoginRequestDTO externalRequestDTO) {
        return userService.loginUser(externalRequestDTO);
    }

}
