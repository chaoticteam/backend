package com.chaoticteam.backend.common;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.chaoticteam.backend.auth.services.CurrentUserService;
import com.chaoticteam.backend.utils.JsonBinder;
import com.chaoticteam.backend.utils.NotFoundException;
import com.chaoticteam.backend.utils.Paging;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Shared CRUD rules of the go-server resources: public listing by username,
 * and create/update/delete restricted to the owner (404 when it is not theirs).
 */
public abstract class OwnedResourceService<T extends OwnedEntity> {

    private final OwnedRepository<T> repository;
    private final CurrentUserService currentUser;
    private final JsonBinder binder;
    private final Sort sort;

    protected OwnedResourceService(OwnedRepository<T> repository, CurrentUserService currentUser,
            JsonBinder binder, Sort sort) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.binder = binder;
        this.sort = sort;
    }

    @Transactional(readOnly = true)
    public List<T> list(String username, Integer limit) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return repository.findByUserUsername(username, Paging.of(limit, sort));
    }

    @Transactional(readOnly = true)
    public T detail(Long id) {
        return owned(id);
    }

    @Transactional
    public T create(T entity) {
        entity.setUser(currentUser.get());
        validate(entity);
        return repository.save(entity);
    }

    @Transactional
    public void createAll(List<T> entities) {
        var user = currentUser.get();
        for (T entity : entities) {
            entity.setUser(user);
            validate(entity);
        }
        repository.saveAll(entities);
    }

    @Transactional
    public T update(Long id, JsonNode patch) {
        T entity = owned(id);
        binder.apply(entity, patch);
        validate(entity);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(owned(id));
    }

    protected void validate(T entity) {
    }

    protected static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private T owned(Long id) {
        return repository.findByIdAndUserUsername(id, currentUser.username())
            .orElseThrow(() -> new NotFoundException("resource not found"));
    }
}
