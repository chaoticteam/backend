package com.chaoticteam.backend.commentaries.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chaoticteam.backend.auth.entities.SiteEntity;
import com.chaoticteam.backend.auth.repsository.SiteRepository;
import com.chaoticteam.backend.auth.services.CurrentUserService;
import com.chaoticteam.backend.commentaries.entities.CommentaryEntity;
import com.chaoticteam.backend.commentaries.repository.CommentariesRepository;
import com.chaoticteam.backend.utils.NotFoundException;

@Service
public class CommentariesService {

    private final CommentariesRepository repository;
    private final SiteRepository sites;
    private final CurrentUserService currentUser;

    public CommentariesService(CommentariesRepository repository, SiteRepository sites, CurrentUserService currentUser) {
        this.repository = repository;
        this.sites = sites;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<CommentaryEntity> list(String site){
        if (site == null || site.isBlank()) {
            return List.of();
        }
        return repository.findBySiteUrl(site);
    }

    @Transactional(readOnly = true)
    public CommentaryEntity detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Commentary not found"));
    }

    @Transactional
    public CommentaryEntity create(String origin, String comment) {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("Origin header is required");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("comment is required");
        }
        SiteEntity site = sites.findByUrl(origin)
            .orElseGet(() -> sites.save(new SiteEntity(null, origin, null)));
        CommentaryEntity entity = new CommentaryEntity(null, currentUser.get(), comment, site);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        CommentaryEntity entity = repository.findByIdAndUserUsername(id, currentUser.username())
            .orElseThrow(() -> new NotFoundException("Commentary not found"));
        repository.delete(entity);
    }

}
