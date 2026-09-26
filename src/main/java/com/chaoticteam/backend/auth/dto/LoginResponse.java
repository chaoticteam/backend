package com.chaoticteam.backend.auth.dto;

/** `user` and `token` match go-server; `refreshToken` is an extra of this API. */
public record LoginResponse(UserResponse user, String token, String refreshToken) {
}
