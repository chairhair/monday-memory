package com.monday.monday_backend.auth.verification;

import com.monday.monday_backend.auth.dto.VerificationResponseDTO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * The Verification Controller validates the connection being made between
 * client and server
 */
@RestController
@RequestMapping("/auth")
public class VerificationController {

    @PostMapping("/verify")
    public VerificationResponseDTO validateToken(Authentication auth) {
        return VerificationResponseDTO.successfulDTO(Map.of("principal", auth.getPrincipal(), "roles", auth.getAuthorities()));
    }

}
