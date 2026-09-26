package com.chaoticteam.backend.auth.services;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.chaoticteam.backend.auth.entities.UserEntity;
import com.chaoticteam.backend.auth.repsository.UserRepository;
import com.chaoticteam.backend.utils.NotFoundException;

@Service
public class CurrentUserService {

    private final UserRepository repository;

    public CurrentUserService(UserRepository repository) {
        this.repository = repository;
    }

    public String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public UserEntity get() {
        return repository.findByUsername(username())
            .orElseThrow(() -> new NotFoundException("user not found"));
    }
}
