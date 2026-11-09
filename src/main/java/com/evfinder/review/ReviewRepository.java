package com.evfinder.review;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO reviews (user_id, station_id, rating, comment)
            VALUES (:userId, :stationId, :rating, :comment) AS incoming
            ON DUPLICATE KEY UPDATE review_id = reviews.review_id
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("userId") long userId,
            @Param("stationId") long stationId,
            @Param("rating") int rating,
            @Param("comment") String comment);

    @Query(value = """
            SELECT r.review_id AS reviewId,
                   u.external_id AS externalUserId,
                   r.rating AS rating,
                   r.comment AS comment,
                   r.created_at AS createdAt
            FROM reviews r
            JOIN users u ON u.user_id = r.user_id
            WHERE r.user_id = :userId AND r.station_id = :stationId
            """, nativeQuery = true)
    Optional<ReviewView> findViewByUserIdAndStationId(
            @Param("userId") long userId,
            @Param("stationId") long stationId);

    @Query(value = """
            SELECT r.review_id AS reviewId,
                   u.external_id AS externalUserId,
                   r.rating AS rating,
                   r.comment AS comment,
                   r.created_at AS createdAt
            FROM reviews r
            JOIN users u ON u.user_id = r.user_id
            WHERE r.station_id = :stationId
              AND r.review_id < :beforeReviewId
            ORDER BY r.review_id DESC
            """, nativeQuery = true)
    List<ReviewView> findViewsByStationIdBeforeReviewId(
            @Param("stationId") long stationId,
            @Param("beforeReviewId") long beforeReviewId,
            Pageable pageable);

    interface ReviewView {

        Long getReviewId();

        String getExternalUserId();

        Integer getRating();

        String getComment();

        Instant getCreatedAt();
    }
}
