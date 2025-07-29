package com.monday.monday_backend.auth.roles;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolesRepository extends CrudRepository<RolesEntity, Long> { }
