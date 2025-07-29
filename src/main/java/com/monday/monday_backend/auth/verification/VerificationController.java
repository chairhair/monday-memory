package com.monday.monday_backend.auth.verification;

import com.monday.monday_backend.auth.dto.VerificationRequestDTO;
import com.monday.monday_backend.auth.dto.VerificationResponseDTO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Verification Controller validates the connection being made between
 * client and server
 */
@RestController
@RequestMapping("/verify")
public class VerificationController {

    @PostMapping("/token")
    public VerificationResponseDTO validateToken(@RequestBody VerificationRequestDTO requestDTO) {
        return VerificationResponseDTO.successfulDTO("hi");
    }

}
