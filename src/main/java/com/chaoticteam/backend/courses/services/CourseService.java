package com.chaoticteam.backend.courses.services;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.chaoticteam.backend.auth.services.CurrentUserService;
import com.chaoticteam.backend.courses.entities.CourseEntity;
import com.chaoticteam.backend.courses.repository.CourseRepository;
import com.chaoticteam.backend.common.OwnedResourceService;
import com.chaoticteam.backend.utils.JsonBinder;

@Service
public class CourseService extends OwnedResourceService<CourseEntity> {

    public CourseService(CourseRepository repository, CurrentUserService currentUser, JsonBinder binder) {
        super(repository, currentUser, binder, Sort.by("id"));
    }

    @Override
    protected void validate(CourseEntity entity) {
        require(entity.getName(), "name");
    }
}
