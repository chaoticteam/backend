package com.chaoticteam.backend.auth.repsository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chaoticteam.backend.auth.entities.TelephoneEntity;

@Repository
public interface TelephoneRepository extends JpaRepository<TelephoneEntity, Long> {
    Optional<TelephoneEntity> findByIdAndProfileEntityId(Long id, Long profileId);
}
