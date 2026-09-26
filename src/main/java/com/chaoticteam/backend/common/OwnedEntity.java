package com.chaoticteam.backend.common;

import com.chaoticteam.backend.auth.entities.UserEntity;

/** An entity that belongs to a user (courses, achievements, projects, galleries). */
public interface OwnedEntity {
    void setUser(UserEntity user);
}
