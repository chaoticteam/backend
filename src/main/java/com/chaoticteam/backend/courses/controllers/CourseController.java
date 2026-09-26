package com.chaoticteam.backend.courses.controllers;

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
import com.chaoticteam.backend.courses.entities.CourseEntity;
import com.chaoticteam.backend.courses.services.CourseService;
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
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "Module courses")
public class CourseController {

    private final CourseService service;
    private final JsonBinder binder;

    public CourseController(CourseService service, JsonBinder binder) {
        this.service = service;
        this.binder = binder;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "list", description = "List the courses of a user (public)")
    public ResponseEntity<List<CourseEntity>> list(
            @Parameter(description = "owner username", example = "admin")
            @RequestParam(required = false) String username,
            @Parameter(description = "max items, omit for all", example = "10")
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(service.list(username, limit));
    }

    @PostMapping
    @Operation(
        summary = "create",
        description = "Create a course owned by the authenticated user",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = {
                    @ExampleObject(name = "create", value = """
                {
                    "name": "Docker",
                    "image": "http://example.com/course-docker.jpg"
                }"""),
                    @ExampleObject(name = "create bulk (?type=bulk)", value = """
                [
                    { "name": "Jenkins", "image": "https://res.cloudinary.com/dd7jrtxu5/image/upload/v1628668392/Courses/Jenkins.png" },
                    { "name": "Node.js", "image": "https://res.cloudinary.com/dd7jrtxu5/image/upload/v1628667208/Courses/Node.js.png" }
                ]""")
                })
        )
    )
    @ApiResponse(responseCode = "204", description = "Created (bulk)")
    public ResponseEntity<CourseEntity> create(
            @Parameter(description = "use `bulk` to send an array and create all items", example = "bulk")
            @RequestParam(required = false) String type,
            @RequestBody JsonNode body) {
        if ("bulk".equals(type)) {
            service.createAll(binder.readList(body, CourseEntity.class));
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(service.create(binder.read(body, CourseEntity.class)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "detail", description = "Get one of your courses by id")
    @ApiResponse(responseCode = "404", description = "Not found or not yours", content = @Content)
    public ResponseEntity<CourseEntity> detail(@PathVariable Long id) {
        return ResponseEntity.ok(service.detail(id));
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "update",
        description = "Partial update: only the fields sent are changed",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "name": "Docker expert"
                }"""))
        )
    )
    @ApiResponse(responseCode = "404", description = "Not found or not yours", content = @Content)
    public ResponseEntity<CourseEntity> update(@PathVariable Long id, @RequestBody JsonNode patch) {
        return ResponseEntity.ok(service.update(id, patch));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "delete", description = "Delete one of your courses")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Not found or not yours", content = @Content)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
