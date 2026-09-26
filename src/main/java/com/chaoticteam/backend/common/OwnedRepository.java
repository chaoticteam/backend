package com.chaoticteam.backend.common;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface OwnedRepository<T extends OwnedEntity> extends JpaRepository<T, Long> {
    List<T> findByUserUsername(String username, Pageable pageable);

    Optional<T> findByIdAndUserUsername(Long id, String username);
}
