package com.monday.monday_backend.auth.verification;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * This helps pull information from our items related to security. For example:
 * - UserRepositoryCheck
 */
@Service
public class VerificationService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();



}
