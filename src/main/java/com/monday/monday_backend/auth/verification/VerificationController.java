package com.monday.monday_backend.auth.verification;

import com.monday.monday_backend.auth.dto.VerificationDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Verification Controller validates the connection being made between
 * client and server
 */
@RestController("/verify")
public class VerificationController {

    @GetMapping("/token")
    public VerificationDTO validateToken() {
        return new VerificationDTO(HttpStatus.ACCEPTED, "");
    }

}
