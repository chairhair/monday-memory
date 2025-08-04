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

    @PostMapping("/upsert")
    public UserResponseDTO upsertUser(@RequestBody UserRequestDTO userRequestDTO) {
        return new UserResponseDTO();
    }

    @DeleteMapping
    public UserResponseDTO deleteUser(@RequestParam(required = true) List<Long> uuids) {
        return new UserResponseDTO();
    }

    @PostMapping
    public List<UserResponseDTO> getUsers(@RequestBody UserSearchRequestDTO userSearchRequestDTO) {
        return new ArrayList<>();
    }
}
