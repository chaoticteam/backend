package com.chaoticteam.backend.galleries.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.chaoticteam.backend.galleries.entities.GalleryEntity;
import com.chaoticteam.backend.galleries.services.GalleryService;
import com.chaoticteam.backend.utils.JsonBinder;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/galleries")
@Tag(name = "Galleries", description = "Module galleries")
public class GalleryController {

    private final GalleryService service;
    private final JsonBinder binder;

    public GalleryController(GalleryService service, JsonBinder binder) {
        this.service = service;
        this.binder = binder;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "list", description = "List the galleries of a user (public)")
    public ResponseEntity<List<GalleryEntity>> list(
            @Parameter(description = "owner username", example = "admin")
            @RequestParam(required = false) String username,
            @Parameter(description = "max items, omit for all", example = "10")
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(service.list(username, limit));
    }

    @PostMapping
    @Operation(
        summary = "create",
        description = "Create a gallery owned by the authenticated user",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "image": "http://example.com/assets/image_1.jpg"
                }"""))
        )
    )
    public ResponseEntity<GalleryEntity> create(
            @RequestBody JsonNode body) {
        return ResponseEntity.ok(service.create(binder.read(body, GalleryEntity.class)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "delete", description = "Delete one of your galleries")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Not found or not yours", content = @Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
