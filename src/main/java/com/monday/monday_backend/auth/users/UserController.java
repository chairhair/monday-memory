package com.monday.monday_backend.auth.users;

import com.monday.monday_backend.auth.dto.UserRequestDTO;
import com.monday.monday_backend.auth.dto.UserResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @PostMapping
    public UserResponseDTO createNewUser(UserRequestDTO userRequestDTO) {
        return new UserResponseDTO();
    }
}
