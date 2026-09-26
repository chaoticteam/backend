package com.chaoticteam.backend.achievements.repository;

import org.springframework.stereotype.Repository;

import com.chaoticteam.backend.achievements.entities.AchievementEntity;
import com.chaoticteam.backend.common.OwnedRepository;

@Repository
public interface AchievementRepository extends OwnedRepository<AchievementEntity> {
}
