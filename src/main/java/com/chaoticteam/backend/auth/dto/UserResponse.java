package com.chaoticteam.backend.auth.dto;

import com.chaoticteam.backend.auth.entities.UserEntity;

public record UserResponse(Long id, String userName, String email, boolean verified, ProfileResponse profile) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.isVerified(),
            ProfileResponse.from(user.getProfileEntity(), user.getId())
        );
    }
}
