package com.chaoticteam.backend.projects.services;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.chaoticteam.backend.auth.services.CurrentUserService;
import com.chaoticteam.backend.projects.entities.ProjectEntity;
import com.chaoticteam.backend.projects.repository.ProjectRepository;
import com.chaoticteam.backend.common.OwnedResourceService;
import com.chaoticteam.backend.utils.JsonBinder;

@Service
public class ProjectService extends OwnedResourceService<ProjectEntity> {

    public ProjectService(ProjectRepository repository, CurrentUserService currentUser, JsonBinder binder) {
        super(repository, currentUser, binder, Sort.by(Sort.Direction.DESC, "startDate"));
    }

    @Override
    protected void validate(ProjectEntity entity) {
        require(entity.getTitle(), "title");
        require(entity.getDescription(), "description");
        if (entity.getStartDate() == null) {
            throw new IllegalArgumentException("startDate is required");
        }
    }
}
