package com.chaoticteam.backend.auth.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chaoticteam.backend.auth.dto.AuthenticationRefresTokenRequest;
import com.chaoticteam.backend.auth.dto.AuthenticationRequest;
import com.chaoticteam.backend.auth.dto.AuthenticationSignUpRequest;
import com.chaoticteam.backend.auth.dto.LoginResponse;
import com.chaoticteam.backend.auth.dto.UserResponse;
import com.chaoticteam.backend.auth.services.AuthCookieService;
import com.chaoticteam.backend.auth.services.CurrentUserService;
import com.chaoticteam.backend.auth.services.JwtService;
import com.chaoticteam.backend.auth.services.UserDetailsServiceImp;
import com.chaoticteam.backend.utils.HandleTransactionException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Module auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImp service;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthCookieService cookies;

    @Autowired
    private CurrentUserService currentUser;

    // Endpoint para login
    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
        summary = "login",
        description = "Login user. Returns the user and a JWT; also sets the `access_token` cookie.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "username": "admin",
                    "password": "password"
                }"""))
        )
    )
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    @HandleTransactionException
    public ResponseEntity<LoginResponse> login(@RequestBody AuthenticationRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(),request.getPassword())
        );
        return authenticated(request.getUsername());
    }

    // Endpoint para signup
    @PostMapping("/signup")
    @SecurityRequirements
    @Operation(
        summary = "signup",
        description = "Signup user. Returns the user and a JWT; also sets the `access_token` cookie.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "userName": "admin",
                    "password": "password",
                    "email": "admin@example.io",
                    "profile": {
                        "firstName": "alex",
                        "lastName": "diaz"
                    }
                }"""))
        )
    )
    @ApiResponse(responseCode = "200", description = "User created")
    @ApiResponse(responseCode = "400", description = "User or email already exists", content = @Content)
    @Transactional(
        rollbackOn = RuntimeException.class
    )
    @HandleTransactionException
    public ResponseEntity<LoginResponse> signup(@RequestBody AuthenticationSignUpRequest request) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode(request.getPassword());

        // Guardar el perfil y el usuario dentro de la misma transacción
        service.saveUser(
            request.getUsername(),
            request.getEmail(),
            encodedPassword,
            request.getProfile().getFirstName(),
            request.getProfile().getLastName()
        );

        // Si todo va bien, generar el token JWT
        return authenticated(request.getUsername());
    }

    // Endpoint para refrescar el token
    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(
        summary = "refresh",
        description = "Refresh token (extra of this API, not in go-server)",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "refreshToken": "eyJhbGci"
                }"""))
        )
    )
    @HandleTransactionException
    public ResponseEntity<LoginResponse> refresh(@RequestBody AuthenticationRefresTokenRequest request) {
        if (!jwtService.validateToken(request.getRefreshToken())) {
            throw new RuntimeException("not valid jwt");
        }
        return authenticated(jwtService.getUsernameFromToken(request.getRefreshToken()));
    }

    // Endpoint getUserData
    @GetMapping("/userdata")
    @Operation(
        summary = "userdata",
        description = "get the authenticated user with profile and telephones"
    )
    @HandleTransactionException
    public ResponseEntity<UserResponse> getUserData(){
        return ResponseEntity.ok(UserResponse.from(currentUser.get()));
    }

    // Endpoint validateCredentials
    @PostMapping("/validatecredetial")
    @Operation(
        summary = "Validate Credential",
        description = "Stores the bearer token as the `access_token` cookie"
    )
    @ApiResponse(responseCode = "204", description = "Cookie set")
    public ResponseEntity<Void> validateCredentials(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization){
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookies.create(authorization.substring(7)).toString())
            .build();
    }

    // Endpoint logout
    @DeleteMapping("/logout")
    @Operation(
        summary = "logout",
        description = "Clears the `access_token` cookie"
    )
    @ApiResponse(responseCode = "204", description = "Cookie cleared")
    public ResponseEntity<Void> logout(){
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
            .build();
    }

    private ResponseEntity<LoginResponse> authenticated(String username) {
        final UserDetails userDetails = service.loadUserByUsername(username);
        final String jwtToken = jwtService.generateToken(userDetails);
        final String jwtRefreshToken = jwtService.generateRefreshToken(userDetails);
        final UserResponse user = UserResponse.from(service.getUserByUsername(username));
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookies.create(jwtToken).toString())
            .body(new LoginResponse(user, jwtToken, jwtRefreshToken));
    }
}
