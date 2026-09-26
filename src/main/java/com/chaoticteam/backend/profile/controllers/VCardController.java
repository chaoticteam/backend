package com.chaoticteam.backend.profile.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chaoticteam.backend.profile.services.VCardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/vcard")
@Tag(name = "Profile", description = "Module profile")
public class VCardController {

    private static final MediaType VCARD = MediaType.parseMediaType("text/vcard;charset=UTF-8");

    private final VCardService service;

    public VCardController(VCardService service) {
        this.service = service;
    }

    @GetMapping("/{username}")
    @SecurityRequirements
    @Operation(summary = "vcard", description = "Download the user's contact card as a .vcf attachment")
    @ApiResponse(responseCode = "404", description = "User not found or without telephone", content = @Content)
    public ResponseEntity<String> vcard(@PathVariable String username) {
        return ResponseEntity.ok()
            .contentType(VCARD)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + username + ".vcf")
            .body(service.create(username));
    }
}
