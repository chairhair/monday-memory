package com.monday.monday_backend.auth.users;

import com.monday.monday_backend.auth.credentials.UserCredentialsEntity;
import com.monday.monday_backend.auth.credentials.UserCredentialsRepository;
import com.monday.monday_backend.auth.roles.RolesEntity;
import com.monday.monday_backend.auth.roles.RolesRepository;
import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.auth.validation.ValidationUtils;
import com.monday.monday_backend.communication.entity.UserExternalAccount;
import com.monday.monday_backend.communication.repo.UserExternalAccountRepository;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.repo.SessionMemoryRepository;
import com.monday.monday_backend.memory.repo.SessionOptionsRepository;
import com.monday.shared.auth.dto.*;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.session.utils.PrincipalType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final UserExternalAccountRepository userExternalAccountRepository;

    private final SessionMemoryRepository sessionMemoryRepository;

    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final static Logger log = LoggerFactory.getLogger(UserService.class);

    @Transactional
    public UserResponseDTO upsertUser(UserRequestDTO dto) {
        if (!ValidationUtils.isEmailLegitimate(dto.emailAddress()) || !ValidationUtils.isPasswordLegitimate(dto.password())) {
            return UserResponseDTO.failedDTO(HttpStatus.UNAUTHORIZED, "Username or passwords are not valid");
        }
        UserEntity existing = null;
        if (dto.uuid() != null) {
            existing = userRepository.findById(dto.uuid()).orElse(null); //
        }
        if (dto.uuid() == null || existing == null){
            existing = userRepository.findByEmail(dto.emailAddress()).orElse(null);
        }

        if (existing != null) {
            Optional<UserEntity> potentialDuplicates = userRepository.findByEmail(dto.emailAddress());
            if (potentialDuplicates.isPresent() && potentialDuplicates.get().getUserId() != existing.getUserId()) {
                return UserResponseDTO.failedDTO(HttpStatus.CONFLICT, "Duplicate email already found.");
            }
            if (!existing.getEmail().equals(dto.emailAddress())) {
                existing.setEmail(dto.emailAddress());
            }
            UserEntity userEntity = userRepository.save(existing);

            // Now we have to pass in the user credentials
            UserCredentialsEntity userCredentials = userCredentialsRepository.findByUser_UserId(existing.getUserId()).orElse(new UserCredentialsEntity());
            if (!passwordEncoder.matches(dto.password(), userCredentials.getPassword())) {
                userCredentials.setPassword(passwordEncoder.encode(dto.password()));
            }
            Set<AccessLevel> rolesPresent = existing.getRoles().stream().map(RolesEntity::getAccessLevel).collect(Collectors.toSet());
            Set<String> tokensList = userCredentials.getTokens().stream().map(TokensEntity::getToken).collect(Collectors.toSet());
            return UserResponseDTO.successfulDTO(userEntity.getEmail(), rolesPresent, tokensList);
        }

        RolesEntity rolesEntity = rolesRepository.findByAccessLevel(AccessLevel.USER).orElseThrow(() -> new RuntimeException("Default role USER not found"));

        UserEntity newUser = saveUser(dto.emailAddress(), passwordEncoder.encode(dto.password()), Set.of(rolesEntity));
        return UserResponseDTO.successfulDTO(newUser.getEmail(), Set.of(AccessLevel.USER), new HashSet<>());
    }

    @Transactional
    public void deleteUsers(List<Long> uuids) {
        List<UserEntity> usersToDelete = userRepository.findAllById(uuids);
        log.info("Deleting users: {}", usersToDelete.stream().map(UserEntity::getEmail).toList());
        userRepository.deleteAll(usersToDelete);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> retrieveUsers(UserSearchRequestDTO userSearchRequestDTO) {
        Page<UserEntity> userPage = userRepository.findByIdIn(userSearchRequestDTO.userIds(), userSearchRequestDTO.toPageable());

        return userPage.get().map(user -> {
            UserCredentialsEntity userCredentials = userCredentialsRepository.findByUser_UserId(user.getUserId()).orElse(null);
            Set<String> tokens = (userCredentials == null) ? null : userCredentials.getTokens().stream().map(TokensEntity::getToken).collect(Collectors.toSet());
            return UserResponseDTO.successfulDTO(
                user.getEmail(),
                user.getRoles().stream().map(RolesEntity::getAccessLevel).collect(Collectors.toSet()),
                tokens);
        }).collect(Collectors.toList());
    }

    @Transactional
    public IdentityResponseDTO identity(ExternalLoginRequestDTO externalLoginRequestDTO) {
        Optional<UserExternalAccount> findExternalAccount = userExternalAccountRepository.findByProviderAndExternalId(externalLoginRequestDTO.provider(), externalLoginRequestDTO.externalId());
        if (findExternalAccount.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "External Login Request doesn't exist");
        }
        UserEntity userEntity = findExternalAccount.get().getUser();
        List<UUID> sessionIds = sessionMemoryRepository.findByPrincipalTypeAndPrincipalId(PrincipalType.USER, userEntity.getUserId().toString()).stream().map(SessionMemoryEntity::getSessionId).toList();
        return new IdentityResponseDTO(userEntity.getUserId().toString(), PrincipalType.USER, sessionIds);
    }

    @Transactional
    public UserResponseDTO loginUser(ExternalLoginRequestDTO externalLoginRequestDTO) {
        Optional<UserExternalAccount> findExternalAccount = userExternalAccountRepository.findByProviderAndExternalId(externalLoginRequestDTO.provider(), externalLoginRequestDTO.externalId());
        UserEntity user;
        Instant now = Instant.now();
        Set<RolesEntity> setOfRoles;
        if (findExternalAccount.isEmpty()) {
            try {
                // If the account is empty, chances are it hasn't been created yet and should be.
                UserExternalAccount externalAccount = new UserExternalAccount();
                setOfRoles = Set.of(rolesRepository.findByAccessLevel(AccessLevel.GUEST).orElseThrow(() -> new RuntimeException("Default role USER not found")));
                user = saveUser(null, null, setOfRoles);
                externalAccount.setUser(user);
                externalAccount.setProvider(externalLoginRequestDTO.provider());
                externalAccount.setExternalId(externalLoginRequestDTO.externalId());
                externalAccount.setCreatedAt(now);
                userExternalAccountRepository.save(externalAccount);
                return UserResponseDTO.successfulDTO(null, Set.of(AccessLevel.GUEST), null);
            } catch (DataIntegrityViolationException dVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not create the account: "+dVE);
            }
        }
        user = findExternalAccount.get().getUser();
        if (user == null) {
            // If we can't find a user, we must log it and save it
            setOfRoles = Set.of(rolesRepository.findByAccessLevel(AccessLevel.GUEST).orElseThrow(() -> new RuntimeException("Default role USER not found")));
            try {
                saveUser(null, null, setOfRoles);
            } catch (DataIntegrityViolationException dVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not create the account: "+dVE);
            }
            return UserResponseDTO.successfulDTO(null, Set.of(AccessLevel.GUEST), null);
        }
        UserCredentialsEntity userCredentials = userCredentialsRepository.findByUser_UserId(user.getUserId()).orElse(null);
        Set<String> tokens = (userCredentials == null) ? null : userCredentials.getTokens().stream().map(TokensEntity::getToken).collect(Collectors.toSet());
        return UserResponseDTO.successfulDTO(user.getEmail(), user.getRoles().stream().map(RolesEntity::getAccessLevel).collect(Collectors.toSet()), tokens);
    }


    private UserEntity saveUser(String email, String Password, Set<RolesEntity> roles) throws DataIntegrityViolationException {
        UserEntity user = new UserEntity();
        user.addRole(rolesRepository.findByAccessLevel(AccessLevel.GUEST).orElseThrow(() -> new RuntimeException("Default role USER not found")));
        user.setEmail(null);
        userRepository.save(user);
        return user;
    }
}
