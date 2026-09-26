package com.chaoticteam.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthenticationSignUpRequest {
    @JsonAlias("userName")
    private String username;
    private String email;
    private String password;

    private ProfileRequest profile;
}