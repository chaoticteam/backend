package com.chaoticteam.backend.achievements.controllers;

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
import com.chaoticteam.backend.achievements.entities.AchievementEntity;
import com.chaoticteam.backend.achievements.services.AchievementService;
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
@RequestMapping("/api/achievements")
@Tag(name = "Achievements", description = "Module achievements")
public class AchievementController {

    private final AchievementService service;
    private final JsonBinder binder;

    public AchievementController(AchievementService service, JsonBinder binder) {
        this.service = service;
        this.binder = binder;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "list", description = "List the achievements of a user (public)")
    public ResponseEntity<List<AchievementEntity>> list(
            @Parameter(description = "owner username", example = "admin")
            @RequestParam(required = false) String username,
            @Parameter(description = "max items, omit for all", example = "10")
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(service.list(username, limit));
    }

    @PostMapping
    @Operation(
        summary = "create",
        description = "Create an achievement owned by the authenticated user",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = {
                    @ExampleObject(name = "create", value = """
                {
                    "year": 2023,
                    "comment": "i'm got a job at DevOps Engineer",
                    "title": "Abstract Development Studio SA"
                }"""),
                    @ExampleObject(name = "create bulk (?type=bulk)", value = """
                [
                    { "year": 2023, "comment": "i'm got a job at DevOps Engineer", "title": "Abstract Development Studio SA" },
                    { "year": 2021, "comment": "i'm got a job at Backend Developer", "title": "SignsCloud" }
                ]""")
                })
        )
    )
    @ApiResponse(responseCode = "204", description = "Created (bulk)")
    public ResponseEntity<AchievementEntity> create(
            @Parameter(description = "use `bulk` to send an array and create all items", example = "bulk")
            @RequestParam(required = false) String type,
            @RequestBody JsonNode body) {
        if ("bulk".equals(type)) {
            service.createAll(binder.readList(body, AchievementEntity.class));
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(service.create(binder.read(body, AchievementEntity.class)));
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "update",
        description = "Partial update: only the fields sent are changed",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "comment": "i'm got a job at Backend Developer"
                }"""))
        )
    )
    @ApiResponse(responseCode = "404", description = "Not found or not yours", content = @Content)
    public ResponseEntity<AchievementEntity> update(@PathVariable Long id, @RequestBody JsonNode patch) {
        return ResponseEntity.ok(service.update(id, patch));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "delete", description = "Delete one of your achievements")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Not found or not yours", content = @Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
