package com.monday.monday_backend.auth.verification;

import com.monday.monday_backend.auth.dto.VerificationResponseDTO;
import com.monday.monday_backend.auth.tokens.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * The Verification Controller validates the connection being made between
 * client and server
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class VerificationController {

    JwtUtil jwtUtil;

    @PostMapping("/verify")
    public VerificationResponseDTO validateToken(Authentication auth) {
        return VerificationResponseDTO.successfulDTO(Map.of("principal", auth.getPrincipal(), "roles", auth.getAuthorities()));
    }

    @PostMapping("/token")
    public VerificationResponseDTO getToken(
            @RequestParam String serviceName,
            @RequestParam String role
    ) {
        return VerificationResponseDTO.successfulDTO(Map.of("token", jwtUtil.generateToken(serviceName, role)));
    }

}
