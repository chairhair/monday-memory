package com.monday.monday_backend.auth.users;

import com.monday.monday_backend.auth.credentials.UserCredentialsEntity;
import com.monday.monday_backend.auth.credentials.UserCredentialsRepository;
import com.monday.monday_backend.auth.filters.JwtService;
import com.monday.monday_backend.auth.roles.RolesEntity;
import com.monday.monday_backend.auth.roles.RolesRepository;
import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.auth.validation.ValidationUtils;
import com.monday.monday_backend.communication.entity.UserExternalAccount;
import com.monday.monday_backend.communication.repo.UserExternalAccountRepository;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.repo.SessionMemoryRepository;
import com.monday.monday_backend.memory.repo.SessionOptionsRepository;
import com.monday.monday_backend.memory.service.LimitsProperties;
import com.monday.monday_backend.payment.PlanDefaultsService;
import com.monday.monday_backend.payment.entity.PricePlanEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.monday_backend.payment.repo.PricePlanRepository;
import com.monday.shared.auth.dto.*;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.auth.utils.ExternalProvider;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionScope;
import com.monday.shared.recording.RecordingScope;
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

    private final JwtService jwtService;
    private final LimitsProperties limits;

    private final UserRepository userRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final UserExternalAccountRepository userExternalAccountRepository;
    private final SessionMemoryRepository sessionMemoryRepository;
    private final PricePlanRepository pricePlanRepository;

    private final PlanDefaultsService planDefaultsService;

    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final static Logger log = LoggerFactory.getLogger(UserService.class);

    @Transactional
    public UserResponseDTO upsertUser(UserRequestDTO dto) {
        UserEntity existing = null;
        UUID availableUUID = null;
        try {
            availableUUID = UUID.fromString(dto.principalKey());
        } catch (Exception ignored) {}

        Optional<UserExternalAccount> userExternalAccount = userExternalAccountRepository.findByProviderAndExternalId(dto.source(), dto.principalKey());
        if (availableUUID != null) {
            existing = userRepository.findById(availableUUID).orElse(null); //
            if (existing == null && userExternalAccount.isPresent()) {
                existing = userExternalAccount.get().getUser();
            }
        }
        if ((availableUUID == null || existing == null) && dto.emailAddress() != null) {
            existing = userRepository.findByEmail(dto.emailAddress()).orElse(null);
            if (existing == null && userExternalAccount.isPresent()) {
                existing = userExternalAccount.get().getUser();
            }
        }

        UserPreferencesDTO userPreferencesDTO = dto.options();


        List<UUID> sessionIds;

        if (existing != null) {
            sessionIds = sessionMemoryRepository.findByPrincipalTypeAndPrincipalId(PrincipalType.USER, dto.principalKey())
                    .stream()
                    .map(SessionMemoryEntity::getSessionId).toList();
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

            // If we hit null on our dtos, we want to make sure that we still return something
            if (userPreferencesDTO == null) {
                userPreferencesDTO = generatePreferenceStats(SessionScope.CHANNEL, RecordingScope.PRIVATE, userEntity);
            }

            if (userExternalAccount.isEmpty()) {
                UserExternalAccount saveAccount = new UserExternalAccount();
                saveAccount.setUser(userEntity);
                saveAccount.setProvider(dto.source());
                saveAccount.setCreatedAt(Instant.now());
                saveAccount.setExternalId(dto.principalKey());
                userExternalAccountRepository.save(saveAccount);
            }

            return UserResponseDTO.successfulDTO(userEntity.getUserId(), sessionIds, userEntity.getEmail(), rolesPresent, tokensList, generateUseStats(existing), userPreferencesDTO);
        }

        RolesEntity rolesEntity = rolesRepository.findByAccessLevel(AccessLevel.USER).orElseThrow(() -> new RuntimeException("Default role USER not found"));

        UserEntity newUser = saveUser(dto.emailAddress(), Set.of(rolesEntity));

        // Now we have to pass in the user credentials
        UserCredentialsEntity userCredentials = new UserCredentialsEntity();
        if (dto.password() != null) {
            userCredentials.setPassword(passwordEncoder.encode(dto.password()));
        }
        userCredentials.setUser(newUser);
        userCredentialsRepository.save(userCredentials);

        // Prior to finishing, we must include a new token as part of our
        VerificationResponseDTO verificationDTO = jwtService.assignToken(new VerificationRequestDTO(
                newUser.getUserId().toString(),
                dto.source().toString(),
                AccessLevel.USER.name(),
                dto.emailAddress(),
                dto.password()
        ));

        HashSet<String> ourTokens = new HashSet<>();
        ourTokens.add((String)verificationDTO.authentication().get("token"));

        // Update all session memory entities that were previously included under our guest.
        List<SessionMemoryEntity> sessionMemoryEntities = sessionMemoryRepository.findByPrincipalTypeAndPrincipalId(PrincipalType.GUEST, dto.principalKey());
        for (SessionMemoryEntity sessionMemory : sessionMemoryEntities) {
            sessionMemory.setPrincipalType(PrincipalType.USER);
            sessionMemory.setUser(newUser);
            sessionMemoryRepository.save(sessionMemory);
        }
        sessionIds = sessionMemoryEntities
                .stream()
                .map(SessionMemoryEntity::getSessionId).toList();

        if (verificationDTO.statusCode() != HttpStatus.OK) {
            return UserResponseDTO.failedDTO(HttpStatus.CONFLICT, "Could not generate a token for our user following token assignment");
        }
        // If we hit null on our dtos, we want to make sure that we still return something
        if (userPreferencesDTO == null) {
            userPreferencesDTO = generatePreferenceStats(SessionScope.CHANNEL, RecordingScope.PRIVATE, newUser);
        }

        if (userExternalAccount.isEmpty()) {
            UserExternalAccount saveAccount = new UserExternalAccount();
            saveAccount.setUser(newUser);
            saveAccount.setProvider(dto.source());
            saveAccount.setCreatedAt(Instant.now());
            saveAccount.setExternalId(dto.principalKey());
            userExternalAccountRepository.save(saveAccount);
        }

        return UserResponseDTO.successfulDTO(newUser.getUserId(), sessionIds, newUser.getEmail(), Set.of(AccessLevel.USER), ourTokens, generateUseStats(newUser), userPreferencesDTO);
    }

    @Transactional
    public void deleteUsers(List<UUID> uuids) {
        List<UserEntity> usersToDelete = userRepository.findAllById(uuids);
        log.info("Deleting users: {}", usersToDelete.stream().map(UserEntity::getEmail).toList());
        userRepository.deleteAll(usersToDelete);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> retrieveUsers(UserSearchRequestDTO userSearchRequestDTO) {
        Page<UserEntity> userPage = userRepository.findByIdIn(userSearchRequestDTO.userIds(), userSearchRequestDTO.toPageable());

        return userPage.get().map(user -> {
            List<UUID> sessionIds = sessionMemoryRepository.findByPrincipalTypeAndPrincipalId(PrincipalType.USER, user.getUserId().toString())
                    .stream()
                    .map(SessionMemoryEntity::getSessionId).toList();
            UserCredentialsEntity userCredentials = userCredentialsRepository.findByUser_UserId(user.getUserId()).orElse(null);

            Set<String> tokens = (userCredentials == null) ? null : userCredentials.getTokens().stream().map(TokensEntity::getToken).collect(Collectors.toSet());
            return UserResponseDTO.successfulDTO(
                user.getUserId(),
                sessionIds,
                user.getEmail(),
                user.getRoles().stream().map(RolesEntity::getAccessLevel).collect(Collectors.toSet()),
                tokens,
                generateUseStats(user),
                generatePreferenceStats(user));
        }).collect(Collectors.toList());
    }

    @Transactional
    public IdentityResponseDTO identity(ExternalLoginRequestDTO externalLoginRequestDTO) {
        Optional<UserExternalAccount> findExternalAccount = userExternalAccountRepository.findByProviderAndExternalId(externalLoginRequestDTO.provider(), externalLoginRequestDTO.externalId());
        if (findExternalAccount.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "External Login Request doesn't exist");
        }
        UserEntity userEntity = findExternalAccount.get().getUser();

        UserPreferencesDTO options = new UserPreferencesDTO(
                userEntity.getUserPreferences().getScope(),
                userEntity.getUserPreferences().getCommScope(),
                userEntity.getUserPreferences().getMaxChunksPerSession(),
                userEntity.getUserPreferences().getMaxTokensPerSession()
        );
        Optional<UserCredentialsEntity> userCredentials = userCredentialsRepository.findByUser_UserId(userEntity.getUserId());
        if (userCredentials.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Can't find any user credentials present.");
        }

        List<TokensEntity> tokens = userCredentials.get().getTokens();
        TokensEntity foundToken = null;

        for (TokensEntity token : tokens) {
            if (ExternalProvider.fromString(token.getToken()).isEmpty()) {
                foundToken = token;
                break;
            }
        }

        if (foundToken == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Can't find any user credentials present.");
        }

        List<UUID> sessionIds = sessionMemoryRepository.findByPrincipalTypeAndPrincipalId(PrincipalType.USER, userEntity.getUserId().toString()).stream().map(SessionMemoryEntity::getSessionId).toList();
        return new IdentityResponseDTO(userEntity.getUserId().toString(), PrincipalType.USER, sessionIds, foundToken.getToken(), generateUseStats(userEntity), options);
    }

    @Transactional
    public UserResponseDTO loginUser(ExternalLoginRequestDTO externalLoginRequestDTO) {
        Optional<UserExternalAccount> findExternalAccount = userExternalAccountRepository.findByProviderAndExternalId(externalLoginRequestDTO.provider(), externalLoginRequestDTO.externalId());
        UserEntity user;
        Instant now = Instant.now();
        Set<RolesEntity> setOfRoles;
        List<UUID> sessionIds;
        if (findExternalAccount.isEmpty()) {
            try {
                sessionIds = sessionMemoryRepository.findByPrincipalTypeAndPrincipalId(PrincipalType.GUEST, externalLoginRequestDTO.externalId())
                        .stream()
                        .map(SessionMemoryEntity::getSessionId).toList();
                // If the account is empty, chances are it hasn't been created yet and should be.
                UserExternalAccount externalAccount = new UserExternalAccount();
                setOfRoles = Set.of(rolesRepository.findByAccessLevel(AccessLevel.GUEST).orElseThrow(() -> new RuntimeException("Default role USER not found")));
                user = saveUser(null, setOfRoles);
                externalAccount.setUser(user);
                externalAccount.setProvider(externalLoginRequestDTO.provider());
                externalAccount.setExternalId(externalLoginRequestDTO.externalId());
                externalAccount.setCreatedAt(now);
                userExternalAccountRepository.save(externalAccount);

                return UserResponseDTO.successfulDTO(null, sessionIds, null, Set.of(AccessLevel.GUEST), null,
                        generateUseStats(user),
                        generatePreferenceStats(SessionScope.CHANNEL, RecordingScope.PRIVATE, user));
            } catch (DataIntegrityViolationException dVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not logint into the account: "+dVE);
            }
        }
        user = findExternalAccount.get().getUser();
        if (user == null) {
            sessionIds = sessionMemoryRepository.findByPrincipalTypeAndPrincipalId(PrincipalType.GUEST, externalLoginRequestDTO.externalId())
                    .stream()
                    .map(SessionMemoryEntity::getSessionId).toList();
            // If we can't find a user, we must log it and save it
            setOfRoles = Set.of(rolesRepository.findByAccessLevel(AccessLevel.GUEST).orElseThrow(() -> new RuntimeException("Default role USER not found")));
            try {
                user = saveUser(null, setOfRoles);
            } catch (DataIntegrityViolationException dVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not create the account: "+dVE);
            }
            return UserResponseDTO.successfulDTO(null, sessionIds, null, Set.of(AccessLevel.GUEST), null,
                    generateUseStats(user),
                    generatePreferenceStats(SessionScope.CHANNEL, RecordingScope.PRIVATE, user)
                    );
        }

        sessionIds = sessionMemoryRepository.findByPrincipalTypeAndPrincipalId(PrincipalType.USER, user.getUserId().toString())
                .stream()
                .map(SessionMemoryEntity::getSessionId).toList();
        UserCredentialsEntity userCredentials = userCredentialsRepository.findByUser_UserId(user.getUserId()).orElse(null);
        Set<String> tokens = (userCredentials == null) ? null : userCredentials.getTokens().stream().map(TokensEntity::getToken).collect(Collectors.toSet());

        UserPreferencesDTO options = new UserPreferencesDTO(
                user.getUserPreferences().getScope(),
                user.getUserPreferences().getCommScope(),
                user.getUserPreferences().getMaxChunksPerSession(),
                user.getUserPreferences().getMaxTokensPerSession()
        );
        return UserResponseDTO.successfulDTO(user.getUserId(), sessionIds, user.getEmail(), user.getRoles().stream().map(RolesEntity::getAccessLevel).collect(Collectors.toSet()), tokens, generateUseStats(user), options);
    }


    private UserEntity saveUser(String email, Set<RolesEntity> roles) throws DataIntegrityViolationException {
        UserEntity user = new UserEntity();
        user.addRole(rolesRepository.findByAccessLevel(AccessLevel.USER).orElseThrow(() -> new RuntimeException("Default role USER not found")));
        for (RolesEntity role : roles) {
            user.addRole(role);
        }
        user.setEmail(email);

        UserPlanEntity userPlan = new UserPlanEntity();
        PricePlanEntity pricePlanEntity = pricePlanRepository.findByCode("FREE").orElseThrow(() -> {
            throw new RuntimeException("Could not find Price plan available");
        });
        userPlan.setUser(user);
        userPlan.setPlan(pricePlanEntity);

        user.setUserPlan(userPlan);

        userRepository.save(user);
        return user;
    }

    private UserUseStatsDTO generateUseStats(UserEntity user) {
        UserPlanEntity userPlan = user.getUserPlan();

        Long tokensUsed = (userPlan == null || userPlan.getTokensUsed() == null) ? 0 : userPlan.getTokensUsed();
        Integer topicsUsed = (userPlan == null || userPlan.getTopicsUsed() == null) ? 0 : userPlan.getTopicsUsed();
        Long tokensUsedMonth = (userPlan == null || userPlan.getTokensUsedMonth() == null) ? 0 :userPlan.getTokensUsedMonth();

        return new UserUseStatsDTO(
                tokensUsed,
                topicsUsed,
                tokensUsedMonth
        );
    }

    private UserPreferencesDTO generatePreferenceStats(UserEntity user) {
        return generatePreferenceStats(null, null, user);
    }

    private UserPreferencesDTO generatePreferenceStats(SessionScope sessionScope, RecordingScope recordingScope, UserEntity user) {
        UserPreferencesEntity prefs = user.getUserPreferences();
        UserPlanEntity userPlan = user.getUserPlan();

        // This is where you map your concrete price plan / flags to "Pro" vs "Free"
        if (userPlan != null && planDefaultsService.isProPlan(userPlan)) {
            return limits.toPrefsDTO(
                sessionScope == null ? prefs.getScope() : sessionScope,
                recordingScope == null ? prefs.getCommScope() : recordingScope,
                EffectivePlan.USER_PRO
            );
        }

        return limits.toPrefsDTO(
            sessionScope == null ? prefs.getScope() : sessionScope,
            recordingScope == null ? prefs.getCommScope() : recordingScope,
            EffectivePlan.USER_FREE
        );
    }
}
