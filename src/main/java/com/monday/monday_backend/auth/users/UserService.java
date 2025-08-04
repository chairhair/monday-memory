package com.monday.monday_backend.auth.users;

import com.monday.monday_backend.auth.dto.UserRequestDTO;
import com.monday.monday_backend.auth.dto.UserResponseDTO;
import com.monday.monday_backend.auth.dto.UserSearchRequestDTO;
import com.monday.monday_backend.auth.roles.AccessLevel;
import com.monday.monday_backend.auth.roles.RolesEntity;
import com.monday.monday_backend.auth.roles.RolesRepository;
import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.auth.validation.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final static Logger log = LoggerFactory.getLogger(UserService.class);

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

    public void deleteUsers(List<Long> uuids) {
        List<UserEntity> usersToDelete = userRepository.findAllById(uuids);
        log.info("Deleting users: {}", usersToDelete.stream().map(UserEntity::getEmail).toList());
        userRepository.deleteAll(usersToDelete);
    }

    public List<UserResponseDTO> retrieveUsers(UserSearchRequestDTO userSearchRequestDTO) {
        Page<UserEntity> userPage = userRepository.findByIdIn(userSearchRequestDTO.userIds(), userSearchRequestDTO.toPageable());
        return userPage.get().map(user -> UserResponseDTO.successfulDTO(
                user.getEmail(),
                user.getServiceName(),
                user.getRoles().stream().map(RolesEntity::getAccessLevel).collect(Collectors.toSet()),
                user.getTokensEntity().stream().map(TokensEntity::getToken).collect(Collectors.toSet())))
                .collect(Collectors.toList());
    }
}
