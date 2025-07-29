package com.monday.monday_backend.auth.tokens;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokensRepository extends CrudRepository<TokensEntity, Long> { }
