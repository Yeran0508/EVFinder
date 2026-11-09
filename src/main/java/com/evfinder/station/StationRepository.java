package com.evfinder.station;

import java.util.Collection;
import java.util.List;

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
            SELECT DISTINCT s.*
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
}
