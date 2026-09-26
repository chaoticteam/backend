package com.chaoticteam.backend.courses.repository;

import org.springframework.stereotype.Repository;

import com.chaoticteam.backend.courses.entities.CourseEntity;
import com.chaoticteam.backend.common.OwnedRepository;

@Repository
public interface CourseRepository extends OwnedRepository<CourseEntity> {
}
