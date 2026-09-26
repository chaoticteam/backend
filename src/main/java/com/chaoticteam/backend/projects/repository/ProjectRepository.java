package com.chaoticteam.backend.projects.repository;

import org.springframework.stereotype.Repository;

import com.chaoticteam.backend.projects.entities.ProjectEntity;
import com.chaoticteam.backend.common.OwnedRepository;

@Repository
public interface ProjectRepository extends OwnedRepository<ProjectEntity> {
}
