package com.chaoticteam.backend.auth.services;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Builds the `access_token` cookie that go-server sets on login/signup. */
@Component
public class AuthCookieService {

    public static final String NAME = "access_token";

    @Value("${app.cookie.secure:true}")
    private boolean secure;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    public ResponseCookie create(String token) {
        return base(token).maxAge(Duration.ofHours(10)).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/");
    }
}
