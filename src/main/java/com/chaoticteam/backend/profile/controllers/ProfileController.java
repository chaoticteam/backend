package com.chaoticteam.backend.profile.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chaoticteam.backend.auth.dto.UserResponse;
import com.chaoticteam.backend.profile.services.ProfileService;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Module profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "profile", description = "Get the public profile (with telephones) of a user")
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    public ResponseEntity<UserResponse> get(
            @Parameter(description = "username", example = "admin")
            @RequestParam(required = false) String username) {
        return ResponseEntity.ok(service.get(username));
    }

    @PatchMapping
    @Operation(
        summary = "profile",
        description = "Partial update of the authenticated user's profile. Accepts camelCase or snake_case (first_name, last_name).",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "first_name": "Alex",
                    "last_name": "Diaz",
                    "photo": "https://example.com/photo.jpg",
                    "bio": "I am Alex Diaz, Software Engineer.",
                    "twitter": "al3xsierra",
                    "linkedin": "al3xdiaz/",
                    "youtube": "al3xdiaz",
                    "github": "https://github.com/al3xdiaz",
                    "gitlab": "https://gitlab.com/al3xdiaz",
                    "discord": "https://discordapp.com/users/1028806986457960488",
                    "website": "https://portfolio.chaoticteam.com/al3xdiaz",
                    "telephone": null,
                    "specialties": "DevOps,Backend,Frontent,Mobile"
                }"""))
        )
    )
    public ResponseEntity<UserResponse> update(@RequestBody JsonNode patch) {
        return ResponseEntity.ok(service.update(patch));
    }
}
