package com.chaoticteam.backend.profile.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chaoticteam.backend.auth.dto.TelephoneResponse;
import com.chaoticteam.backend.auth.entities.ProfileEntity;
import com.chaoticteam.backend.auth.entities.TelephoneEntity;
import com.chaoticteam.backend.auth.repsository.TelephoneRepository;
import com.chaoticteam.backend.auth.services.CurrentUserService;
import com.chaoticteam.backend.profile.dto.TelephoneRequest;
import com.chaoticteam.backend.utils.NotFoundException;

@Service
public class TelephoneService {

    private final TelephoneRepository repository;
    private final CurrentUserService currentUser;

    public TelephoneService(TelephoneRepository repository, CurrentUserService currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @Transactional
    public TelephoneResponse create(TelephoneRequest request) {
        if (request.phoneNumber() == null || request.phoneNumber().isBlank()) {
            throw new IllegalArgumentException("phoneNumber is required");
        }
        TelephoneEntity entity = new TelephoneEntity();
        entity.setPhoneNumber(request.phoneNumber());
        entity.setCountryCode(request.countryCode());
        entity.setWhatsapp(request.whatsapp());
        entity.setTelegram(request.telegram());
        entity.setProfileEntity(profile());
        return TelephoneResponse.from(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        TelephoneEntity entity = repository.findByIdAndProfileEntityId(id, profile().getId())
            .orElseThrow(() -> new NotFoundException("telephone not found"));
        repository.delete(entity);
    }

    private ProfileEntity profile() {
        ProfileEntity profile = currentUser.get().getProfileEntity();
        if (profile == null) {
            throw new NotFoundException("profile not found");
        }
        return profile;
    }
}
