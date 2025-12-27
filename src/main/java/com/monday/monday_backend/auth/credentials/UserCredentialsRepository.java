package com.monday.monday_backend.auth.credentials;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentialsEntity, UUID> {

    Optional<UserCredentialsEntity> findByUser_UserId(UUID userId);

    @Query("""
        select uc
        from UserCredentialsEntity uc
        join fetch uc.tokens t
        where t.token = :token
    """)
    Optional<UserCredentialsEntity> findByTokenWithTokens(String token);
}
