package com.monday.monday_backend.auth.users;

import com.monday.monday_backend.auth.dto.UserRequestDTO;
import com.monday.monday_backend.auth.dto.UserResponseDTO;
import com.monday.monday_backend.auth.dto.UserSearchRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
}
