package com.monday.monday_backend.communication.repo;

import com.monday.monday_backend.communication.entity.UserExternalAccount;
import com.monday.monday_backend.communication.utils.ExternalProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserExternalAccountRepository extends JpaRepository<UserExternalAccount, Long> {
    Optional<UserExternalAccount> findByProviderAndExternalId(ExternalProvider provider, String externalId);
}
