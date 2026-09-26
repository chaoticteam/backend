package com.chaoticteam.backend.achievements.services;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.chaoticteam.backend.auth.services.CurrentUserService;
import com.chaoticteam.backend.achievements.entities.AchievementEntity;
import com.chaoticteam.backend.achievements.repository.AchievementRepository;
import com.chaoticteam.backend.common.OwnedResourceService;
import com.chaoticteam.backend.utils.JsonBinder;

@Service
public class AchievementService extends OwnedResourceService<AchievementEntity> {

    public AchievementService(AchievementRepository repository, CurrentUserService currentUser, JsonBinder binder) {
        super(repository, currentUser, binder, Sort.by(Sort.Direction.DESC, "year"));
    }

    @Override
    protected void validate(AchievementEntity entity) {
        if (entity.getYear() <= 0) {
            throw new IllegalArgumentException("year must be greater than 0");
        }
        require(entity.getComment(), "comment");
        require(entity.getTitle(), "title");
    }
}
