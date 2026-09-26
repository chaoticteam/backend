package com.chaoticteam.backend.projects.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.chaoticteam.backend.projects.entities.ProjectEntity;
import com.chaoticteam.backend.projects.services.ProjectService;
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
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "Module projects")
public class ProjectController {

    private final ProjectService service;
    private final JsonBinder binder;

    public ProjectController(ProjectService service, JsonBinder binder) {
        this.service = service;
        this.binder = binder;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "list", description = "List the projects of a user (public)")
    public ResponseEntity<List<ProjectEntity>> list(
            @Parameter(description = "owner username", example = "admin")
            @RequestParam(required = false) String username,
            @Parameter(description = "max items, omit for all", example = "10")
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(service.list(username, limit));
    }

    @PostMapping
    @Operation(
        summary = "create",
        description = "Create a project owned by the authenticated user",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "title": "portfolio",
                    "description": "this is a portfolio made with golang and react",
                    "startDate": "2024-06-21T09:49:48.385Z"
                }"""))
        )
    )
    public ResponseEntity<ProjectEntity> create(
            @RequestBody JsonNode body) {
        return ResponseEntity.ok(service.create(binder.read(body, ProjectEntity.class)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "detail", description = "Get one of your projects by id")
    @ApiResponse(responseCode = "404", description = "Not found or not yours", content = @Content)
    public ResponseEntity<ProjectEntity> detail(@PathVariable Long id) {
        return ResponseEntity.ok(service.detail(id));
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "update",
        description = "Partial update: only the fields sent are changed",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "url": "http://example.com/assets/image.jpg"
                }"""))
        )
    )
    @ApiResponse(responseCode = "404", description = "Not found or not yours", content = @Content)
    public ResponseEntity<ProjectEntity> update(@PathVariable Long id, @RequestBody JsonNode patch) {
        return ResponseEntity.ok(service.update(id, patch));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "delete", description = "Delete one of your projects")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Not found or not yours", content = @Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
