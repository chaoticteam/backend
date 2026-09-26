package com.chaoticteam.backend.commentaries.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chaoticteam.backend.commentaries.dto.CommentaryRequest;
import com.chaoticteam.backend.commentaries.dto.CommentaryResponse;
import com.chaoticteam.backend.commentaries.services.CommentariesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/commentaries")
@Tag(name = "Commentaries", description = "Module commentaries (scoped to the site in the Origin header)")
public class CommentaryController {

    private final CommentariesService service;

    public CommentaryController(CommentariesService service) {
        this.service = service;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(
        summary = "list",
        description = "List the commentaries of the site given in the Origin header"
    )
    public ResponseEntity<List<CommentaryResponse>> list(
            @Parameter(description = "site origin", example = "http://localhost:3000")
            @RequestHeader(value = "Origin", required = false) String origin){
        return ResponseEntity.ok(service.list(origin).stream().map(CommentaryResponse::from).toList());
    }

    @PostMapping
    @Operation(
        summary = "create",
        description = "Create a commentary for the site given in the Origin header",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                {
                    "comment": "Lorem Ipsum is simply dummy text of the printing and typesetting industry."
                }"""))
        )
    )
    public ResponseEntity<CommentaryResponse> create(
            @Parameter(description = "site origin", example = "http://localhost:3000")
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestBody CommentaryRequest request){
        return ResponseEntity.ok(CommentaryResponse.from(service.create(origin, request.comment())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "detail", description = "Get a commentary by id")
    @ApiResponse(responseCode = "404", description = "Commentary not found", content = @Content)
    public ResponseEntity<CommentaryResponse> detail(@PathVariable Long id){
        return ResponseEntity.ok(CommentaryResponse.from(service.detail(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "delete", description = "Delete a commentary (only its author can)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Commentary not found", content = @Content)
    public ResponseEntity<Void> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
