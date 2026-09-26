package com.chaoticteam.backend.profile.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chaoticteam.backend.auth.dto.TelephoneResponse;
import com.chaoticteam.backend.profile.dto.TelephoneRequest;
import com.chaoticteam.backend.profile.services.TelephoneService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/telephone")
@Tag(name = "Profile", description = "Module profile")
public class TelephoneController {

    private final TelephoneService service;

    public TelephoneController(TelephoneService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
        summary = "create telephone",
        description = "Add a telephone to the authenticated user's profile",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "phoneNumber": "87654321",
                    "countryCode": "504",
                    "whatsapp": true
                }"""))
        )
    )
    public ResponseEntity<TelephoneResponse> create(@RequestBody TelephoneRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "delete telephone", description = "Delete one of your telephones")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Not found or not yours", content = @Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
