package com.monday.monday_backend.auth.roles;

public enum AccessLevel {
    INTERNAL_SERVICE,
    READ_ONLY,
    WRITE,
    ADMIN,
    SCHEDULER,
    ANALYTICS,
    EXTERNAL_CLIENT,
    SYSTEM_MAINTAINER,
    USER,
    GUEST
}
