package com.chaoticteam.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationRequest {
    @JsonAlias("userName")
    private String username;
    private String password;
}