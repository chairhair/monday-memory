package com.monday.monday_backend.auth.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    public Optional<UserEntity> findByEmailAndPassword(String email, String password);

    public Optional<UserEntity> findByEmailAndServiceName(String email, String serviceName);

    @Query("SELECT u FROM UserEntity u WHERE u.id IN :ids")
    Page<UserEntity> findByIdIn(@Param("ids") List<Long> ids, Pageable pageable);

}
