package com.chaoticteam.backend.commentaries.dto;

import com.chaoticteam.backend.auth.entities.SiteEntity;
import com.chaoticteam.backend.commentaries.entities.CommentaryEntity;

public record CommentaryResponse(Long id, Long userId, String comment, SiteEntity site) {

    public static CommentaryResponse from(CommentaryEntity entity) {
        return new CommentaryResponse(
            entity.getId(),
            entity.getUser() == null ? null : entity.getUser().getId(),
            entity.getComment(),
            entity.getSite()
        );
    }
}
