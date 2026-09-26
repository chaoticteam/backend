package com.chaoticteam.backend.commentaries.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chaoticteam.backend.commentaries.entities.CommentaryEntity;

@Repository
public interface CommentariesRepository extends JpaRepository<CommentaryEntity,Long>,JpaSpecificationExecutor<CommentaryEntity> {
    @Query("SELECT c FROM CommentaryEntity c JOIN c.site s WHERE s.url = :siteUrl")
    List<CommentaryEntity> findBySiteUrl(@Param("siteUrl") String siteUrl);

    Optional<CommentaryEntity> findByIdAndUserUsername(Long id, String username);
}
