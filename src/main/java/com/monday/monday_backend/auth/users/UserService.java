package com.monday.monday_backend.auth.users;

import com.monday.monday_backend.auth.dto.UserRequestDTO;
import com.monday.monday_backend.auth.dto.UserResponseDTO;
import com.monday.monday_backend.auth.roles.AccessLevel;
import com.monday.monday_backend.auth.roles.RolesEntity;
import com.monday.monday_backend.auth.roles.RolesRepository;
import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.auth.validation.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO upsertUser(UserRequestDTO dto) {
        if (!ValidationUtils.isEmailLegitimate(dto.emailAddress()) || !ValidationUtils.isPasswordLegitimate(dto.password())) {
            return UserResponseDTO.failedDTO(HttpStatus.UNAUTHORIZED.value(), "Username or passwords are not valid");
        }
        UserEntity existing = null;
        if (dto.uuid() != null) {
            existing = userRepository.findById(dto.uuid()).orElse(null); //
        }
        if (dto.uuid() == null || existing == null){
            existing = userRepository.findByEmailAndServiceName(dto.emailAddress(), dto.serviceName()).orElse(null);
        }

        if (existing != null) {
            Optional<UserEntity> potentialDuplicates = userRepository.findByEmailAndServiceName(dto.emailAddress(), dto.serviceName());
            if (potentialDuplicates.isPresent() && potentialDuplicates.get().getUser_id() != existing.getUser_id()) {
                return UserResponseDTO.failedDTO(HttpStatus.CONFLICT.value(), "Duplicate email already found.");
            }
            if (!existing.getEmail().equals(dto.emailAddress())) {
                existing.setEmail(dto.emailAddress());
            }
            if (!passwordEncoder.matches(dto.password(), existing.getPassword())) {
                existing.setPassword(passwordEncoder.encode(dto.password()));
            }
            UserEntity userEntity = userRepository.save(existing);
            Set<AccessLevel> rolesPresent = existing.getRoles().stream().map(RolesEntity::getAccessLevel).collect(Collectors.toSet());
            Set<String> tokensList = existing.getTokensEntity().stream().map(TokensEntity::getToken).collect(Collectors.toSet());
            return UserResponseDTO.successfulDTO(userEntity.getEmail(), userEntity.getServiceName(), rolesPresent, tokensList);
        }

        RolesEntity rolesEntity = rolesRepository.findByAccessLevel(AccessLevel.USER).orElseThrow(() -> new RuntimeException("Default role USER not found"));

        UserEntity newUser = UserEntity.builder()
                .email(dto.emailAddress())
                .serviceName(dto.serviceName())
                .password(passwordEncoder.encode(dto.password()))
                .roles(Set.of(rolesEntity))
                .build();
        userRepository.save(newUser);
        return UserResponseDTO.successfulDTO(newUser.getEmail(), newUser.getServiceName(), Set.of(AccessLevel.USER), new HashSet<>());
    }
}
