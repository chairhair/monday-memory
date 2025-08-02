package com.monday.monday_backend.auth.verification;

import com.monday.monday_backend.auth.dto.VerificationResponseDTO;
import com.monday.monday_backend.auth.tokens.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * The Verification Controller validates the connection being made between
 * client and server
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class VerificationController {

    private final JwtUtil jwtUtil;

    @PostMapping("/verify")
    public VerificationResponseDTO validateToken(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authentication found in context.");
        }
        return VerificationResponseDTO.successfulDTO(Map.of(
                "principal", auth.getName(),
                "roles", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        ));
    }

    @PostMapping("/token")
    public VerificationResponseDTO getToken(
            @RequestParam String serviceName,
            @RequestParam String role
    ) {
        return VerificationResponseDTO.successfulDTO(Map.of("token", jwtUtil.generateToken(serviceName, role)));
    }

}
