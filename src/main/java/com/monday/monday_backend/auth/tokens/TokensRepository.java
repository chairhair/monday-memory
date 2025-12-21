package com.monday.monday_backend.auth.tokens;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokensRepository extends CrudRepository<TokensEntity, UUID> {

    Optional<TokensEntity> findByToken(String token);

    boolean existsByTokenAndExpiredTrue(String token);
    boolean existsByTokenAndRevokedTrue(String token);

    List<TokensEntity> findByExpiredFalseAndTimeCreatedBefore(Instant cutoff);

}
