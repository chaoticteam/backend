package com.chaoticteam.backend.galleries.repository;

import org.springframework.stereotype.Repository;

import com.chaoticteam.backend.galleries.entities.GalleryEntity;
import com.chaoticteam.backend.common.OwnedRepository;

@Repository
public interface GalleryRepository extends OwnedRepository<GalleryEntity> {
}
