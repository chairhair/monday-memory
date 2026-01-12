package com.monday.monday_backend.auth.users;

import com.monday.monday_backend.auth.credentials.UserCredentialsEntity;
import com.monday.monday_backend.auth.credentials.UserCredentialsRepository;
import com.monday.monday_backend.auth.filters.JwtService;
import com.monday.monday_backend.auth.principal.PrincipalResolver;
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
    private final PrincipalResolver principalResolver;

    private final PasswordEncoder passwordEncoder;

    private final RolesRepository rolesRepository;
    private final static Logger log = LoggerFactory.getLogger(UserService.class);

    @Transactional
    public UserResponseDTO upsertUser(UserRequestDTO dto) {

        UUID principalUuid = parseUuidOrNull(dto.principalKey());

        // Primary lookup: external account (provider + externalId)
        Optional<UserExternalAccount> externalOpt =
                userExternalAccountRepository.findByProviderAndExternalId(dto.source(), dto.principalKey());

        // Resolve existing user
        UserEntity user = resolveExistingUser(principalUuid, dto.emailAddress(), externalOpt);

        // Email duplicate check (only if email provided)
        if (dto.emailAddress() != null) {
            Optional<UserEntity> emailOwner = userRepository.findByEmail(dto.emailAddress());
            if (emailOwner.isPresent() && (user == null || !emailOwner.get().getUserId().equals(user.getUserId()))) {
                return UserResponseDTO.failedDTO(HttpStatus.CONFLICT, "Duplicate email found.");
            }
        }

        boolean isNewUser = (user == null);
        if (isNewUser) {
            user = createDefaultUser(dto);
        }

        // Enforce / set email rules
        UserResponseDTO emailConflict = applyEmailRules(user, dto.emailAddress());
        if (emailConflict != null) return emailConflict;

        // Preferences (ensure non-null)
        UserPreferencesDTO prefs = dto.options();
        if (prefs == null) {
            prefs = generatePreferenceStats(SessionScope.CHANNEL, RecordingScope.PRIVATE, user);
        }
        attachAndSavePreferences(user, prefs);

        // Credentials + token behavior
        CredentialResult credResult = ensureCredentialsAndToken(user, dto);

        if (!credResult.ok()) {
            return UserResponseDTO.failedDTO(HttpStatus.CONFLICT, credResult.errorMessage());
        }

        // Ensure external account row exists
        ensureExternalAccount(user, dto, externalOpt);

        // Migrate sessions if this was a guest→user conversion
        List<UUID> sessionIds;
        if (isNewUser) {
            migrateGuestSessionsToUser(dto.principalKey(), user);
        }
        sessionIds = sessionMemoryRepository.findByUser_UserId(user.getUserId())
                .stream()
                .map(SessionMemoryEntity::getSessionId)
                .toList();

        // Roles + tokens for response
        Set<AccessLevel> roles = user.getRoles().stream()
                .map(RolesEntity::getAccessLevel)
                .collect(Collectors.toSet());

        Set<String> tokens = credResult.tokens();

        return UserResponseDTO.successfulDTO(
                user.getUserId(),
                sessionIds,
                user.getEmail(),
                roles,
                tokens,
                generateUseStats(user),
                prefs
        );
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
        if (userEntity == null) {
            throw new IllegalStateException("Could not find user associated with the user!!!");
        }

        UserPreferencesDTO options = new UserPreferencesDTO(
                userEntity.getUserPreferences().getScope(),
                userEntity.getUserPreferences().getCommScope(),
                userEntity.getUserPreferences().getMaxChunksPerSession(),
                userEntity.getUserPreferences().getMaxTokensPerSession()
        );
        UserCredentialsEntity userCredentials = userCredentialsRepository.findByUser_UserId(userEntity.getUserId()).orElse(null);
        EffectivePlan plan = principalResolver.determineEffectivePlan(userEntity, userEntity.getUserPlan(), userCredentials);
        if (userCredentials == null && plan == EffectivePlan.GUEST_FREE) {
            return new IdentityResponseDTO(userEntity.getUserId().toString(), plan, null, null, generateUseStats(userEntity), options);
        }
        if (userCredentials == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Can't find any user credentials present.");
        }

        List<TokensEntity> tokens = userCredentials.getTokens();
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
        return new IdentityResponseDTO(userEntity.getUserId().toString(), plan, sessionIds, foundToken.getToken(), generateUseStats(userEntity), options);
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

    private VerificationResponseDTO generateJWT(UserEntity newUser, UserRequestDTO dto) {
        return jwtService.assignToken(new VerificationRequestDTO(
                newUser.getUserId().toString(),
                dto.source().toString(),
                AccessLevel.USER.name(),
                dto.emailAddress(),
                dto.password()
        ), passwordEncoder);
    }

    private UUID parseUuidOrNull(String s) {
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private UserEntity resolveExistingUser(
            UUID principalUuid,
            String email,
            Optional<UserExternalAccount> externalOpt
    ) {
        // 1) by UUID (if principalKey happens to be a userId)
        if (principalUuid != null) {
            UserEntity byId = userRepository.findById(principalUuid).orElse(null);
            if (byId != null) return byId;
        }

        // 2) by external account if present
        if (externalOpt.isPresent()) {
            return externalOpt.get().getUser();
        }

        // 3) by email if present
        if (email != null) {
            return userRepository.findByEmail(email).orElse(null);
        }

        return null;
    }

    private UserEntity createDefaultUser(UserRequestDTO dto) {
        RolesEntity defaultRole = rolesRepository.findByAccessLevel(AccessLevel.USER)
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));
        return saveUser(dto.emailAddress(), Set.of(defaultRole));
    }

    /**
     * Returns a failed DTO if email rules are violated; otherwise null.
     */
    private UserResponseDTO applyEmailRules(UserEntity user, String email) {
        if (email == null) return null;

        if (user.getEmail() == null) {
            user.setEmail(email);
            userRepository.save(user);
            return null;
        }

        if (!user.getEmail().equals(email)) {
            return UserResponseDTO.failedDTO(HttpStatus.CONFLICT, "Email is different from what was previously set.");
        }

        return null;
    }

    private void attachAndSavePreferences(UserEntity user, UserPreferencesDTO prefs) {
        UserPreferencesEntity existing = user.getUserPreferences();

        if (existing == null) {
            existing = new UserPreferencesEntity();
            existing.setUser(user);
            user.setUserPreferences(existing);
        }

        // copy fields onto the existing row
        existing.setScope(prefs.scope());
        existing.setCommScope(prefs.comScope());
        existing.setMaxChunksPerSession(prefs.maxChunksPerSession());
        existing.setMaxTokensPerSession(prefs.maxTokensPerSession());

        userRepository.save(user); // with cascade this will persist/merge prefs
    }

    private void ensureExternalAccount(UserEntity user, UserRequestDTO dto, Optional<UserExternalAccount> externalOpt) {
        if (externalOpt.isPresent()) return;

        UserExternalAccount acc = new UserExternalAccount();
        acc.setUser(user);
        acc.setProvider(dto.source());
        acc.setExternalId(dto.principalKey());
        acc.setCreatedAt(Instant.now());
        userExternalAccountRepository.save(acc);
    }

    private record CredentialResult(boolean ok, String errorMessage, Set<String> tokens) { }

    private CredentialResult ensureCredentialsAndToken(UserEntity user, UserRequestDTO dto) {
        UserCredentialsEntity creds =
                userCredentialsRepository.findByUser_UserId(user.getUserId()).orElse(null);

        boolean isNewCreds = (creds == null);
        if (isNewCreds) {
            VerificationResponseDTO verification = generateJWT(user, dto);
            if (verification.statusCode() != HttpStatus.OK) {
                return new CredentialResult(false, "Could not generate a token for our user following token assignment", Set.of());
            }

            String token = (String) verification.authentication().get("token");
            return new CredentialResult(true, null, Set.of(token));
        }

        Set<String> tokens = creds.getTokens() == null
                ? Set.of()
                : creds.getTokens().stream().map(TokensEntity::getToken).collect(Collectors.toSet());

        return new CredentialResult(true, null, tokens);
    }

    private void migrateGuestSessionsToUser(String guestKey, UserEntity user) {
        // Best: bulk update in repository (see below). For now, keep behavior.
        List<SessionMemoryEntity> sessions =
                sessionMemoryRepository.findByPrincipalTypeAndPrincipalId(PrincipalType.GUEST, guestKey);

        for (SessionMemoryEntity s : sessions) {
            s.setPrincipalType(PrincipalType.USER);
            s.setUser(user);
        }
        sessionMemoryRepository.saveAll(sessions);
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
