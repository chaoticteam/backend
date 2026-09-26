package com.chaoticteam.backend.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Binds raw JSON bodies to entities. Used for PATCH (partial merge) and for
 * endpoints that accept either one object or an array (?type=bulk).
 */
@Component
public class JsonBinder {

    private final ObjectMapper mapper;

    public JsonBinder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public <T> T read(JsonNode body, Class<T> type) {
        return mapper.convertValue(body, type);
    }

    public <T> List<T> readList(JsonNode body, Class<T> type) {
        if (!body.isArray()) {
            throw new IllegalArgumentException("body must be a JSON array");
        }
        return mapper.convertValue(body, mapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    /**
     * Merges only the fields present in the patch into the target. JSON null
     * values are ignored (same behavior as the go-server decoder).
     */
    public <T> T apply(T target, JsonNode patch) {
        if (!patch.isObject()) {
            throw new IllegalArgumentException("body must be a JSON object");
        }
        ObjectNode clean = ((ObjectNode) patch).deepCopy();
        List<String> nulls = new ArrayList<>();
        clean.fields().forEachRemaining(entry -> {
            if (entry.getValue().isNull()) {
                nulls.add(entry.getKey());
            }
        });
        clean.remove(nulls);
        try {
            return mapper.readerForUpdating(target).readValue(clean);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid body: " + e.getMessage());
        }
    }
}
