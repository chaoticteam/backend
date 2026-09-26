package com.chaoticteam.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chaoticteam.backend.utils.HandleTransactionException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/")
@Tag(name = "Config", description = "Module basic config")
public class ConfigController {

    @Value("${config.version}")
    private String version;

    @GetMapping("/health")
    @Operation(
        summary = "API Health check",
        description = "Health check"
    )
    @HandleTransactionException
    public Void healthVoid() {
        return null;
    }

    @GetMapping("/version")
    @Operation(
        summary = "API version",
        description = "return api version"
    )
    @HandleTransactionException
    public ResponseEntity<String> getVersion(){
        return ResponseEntity.ok(version);
    }
}
