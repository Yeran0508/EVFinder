package com.evfinder.station;

import java.util.Collection;
import java.util.List;
import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findByCityAndStationIdGreaterThanOrderByStationIdAsc(
            String city,
            long stationId,
            Pageable pageable);

    @Query(value = """
            SELECT s.*
            FROM charging_stations s
            JOIN station_connectors sc ON sc.station_id = s.station_id
            JOIN connectors c ON c.connector_id = sc.connector_id
            WHERE s.city = :city
              AND c.connector_name = :connectorType
              AND s.station_id > :afterStationId
            ORDER BY s.station_id
            """, nativeQuery = true)
    List<Station> findByCityAndConnectorTypeAfterStationId(
            @Param("city") String city,
            @Param("connectorType") String connectorType,
            @Param("afterStationId") long afterStationId,
            Pageable pageable);

    List<Station> findAllByExternalIdIn(Collection<String> externalIds);

    @Query(value = """
            SELECT s.station_id AS stationId,
                   AVG(r.rating) AS averageRating,
                   COUNT(r.review_id) AS reviewCount
            FROM charging_stations s
            LEFT JOIN reviews r ON r.station_id = s.station_id
            WHERE s.city = :city
              AND (:connectorType IS NULL OR EXISTS (
                    SELECT 1
                    FROM station_connectors sc
                    JOIN connectors c ON c.connector_id = sc.connector_id
                    WHERE sc.station_id = s.station_id
                      AND c.connector_name = :connectorType
              ))
            GROUP BY s.station_id
            HAVING :afterStationId = 0
                OR (:sortOrder = 'desc' AND (
                    COALESCE(AVG(r.rating), -1) < :afterRating
                    OR (COALESCE(AVG(r.rating), -1) = :afterRating
                        AND s.station_id > :afterStationId)
                ))
                OR (:sortOrder = 'asc' AND (
                    COALESCE(AVG(r.rating), 6) > :afterRating
                    OR (COALESCE(AVG(r.rating), 6) = :afterRating
                        AND s.station_id > :afterStationId)
                ))
            ORDER BY
                CASE WHEN :sortOrder = 'desc' THEN COALESCE(AVG(r.rating), -1) END DESC,
                CASE WHEN :sortOrder = 'asc' THEN COALESCE(AVG(r.rating), 6) END ASC,
                s.station_id ASC
            """, nativeQuery = true)
    List<StationRatingView> findByCityOrderByRating(
            @Param("city") String city,
            @Param("connectorType") String connectorType,
            @Param("sortOrder") String sortOrder,
            @Param("afterRating") BigDecimal afterRating,
            @Param("afterStationId") long afterStationId,
            Pageable pageable);

    @Query(value = """
            SELECT AVG(r.rating) AS averageRating,
                   COUNT(r.review_id) AS reviewCount
            FROM reviews r
            WHERE r.station_id = :stationId
            """, nativeQuery = true)
    StationRatingSummary findRatingSummaryByStationId(@Param("stationId") long stationId);

    interface StationRatingView {
        Long getStationId();

        BigDecimal getAverageRating();

        Long getReviewCount();
    }

    interface StationRatingSummary {
        BigDecimal getAverageRating();

        Long getReviewCount();
    }
}
