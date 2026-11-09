package com.evfinder.review;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByExternalId(String externalId);

    @Modifying
    @Query(value = """
            INSERT INTO users (external_id, display_name)
            VALUES (:externalId, :displayName) AS incoming
            ON DUPLICATE KEY UPDATE external_id = incoming.external_id
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("externalId") String externalId,
            @Param("displayName") String displayName);
}
