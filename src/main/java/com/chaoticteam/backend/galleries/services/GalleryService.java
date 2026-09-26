package com.chaoticteam.backend.galleries.services;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.chaoticteam.backend.auth.services.CurrentUserService;
import com.chaoticteam.backend.galleries.entities.GalleryEntity;
import com.chaoticteam.backend.galleries.repository.GalleryRepository;
import com.chaoticteam.backend.common.OwnedResourceService;
import com.chaoticteam.backend.utils.JsonBinder;

@Service
public class GalleryService extends OwnedResourceService<GalleryEntity> {

    public GalleryService(GalleryRepository repository, CurrentUserService currentUser, JsonBinder binder) {
        super(repository, currentUser, binder, Sort.by("id"));
    }

    @Override
    protected void validate(GalleryEntity entity) {
        require(entity.getImage(), "image");
    }
}
