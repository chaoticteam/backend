package com.chaoticteam.backend.profile.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chaoticteam.backend.auth.dto.UserResponse;
import com.chaoticteam.backend.auth.entities.ProfileEntity;
import com.chaoticteam.backend.auth.entities.UserEntity;
import com.chaoticteam.backend.auth.repsository.ProfileRepository;
import com.chaoticteam.backend.auth.repsository.UserRepository;
import com.chaoticteam.backend.auth.services.CurrentUserService;
import com.chaoticteam.backend.utils.JsonBinder;
import com.chaoticteam.backend.utils.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class ProfileService {

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final CurrentUserService currentUser;
    private final JsonBinder binder;

    public ProfileService(UserRepository users, ProfileRepository profiles, CurrentUserService currentUser, JsonBinder binder) {
        this.users = users;
        this.profiles = profiles;
        this.currentUser = currentUser;
        this.binder = binder;
    }

    @Transactional(readOnly = true)
    public UserEntity findUser(String username) {
        return users.findByUsername(username == null ? "" : username)
            .orElseThrow(() -> new NotFoundException("user not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse get(String username) {
        return UserResponse.from(findUser(username));
    }

    @Transactional
    public UserResponse update(JsonNode patch) {
        UserEntity user = currentUser.get();
        ProfileEntity profile = user.getProfileEntity();
        if (profile == null) {
            throw new NotFoundException("profile not found");
        }
        binder.apply(profile, patch);
        profiles.save(profile);
        return UserResponse.from(user);
    }
}
