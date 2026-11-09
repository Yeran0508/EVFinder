package com.evfinder.station;

import java.util.Collection;
import java.util.List;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectorRepository extends JpaRepository<Connector, Long> {

    @Query(value = """
            SELECT sc.station_id AS stationId, c.connector_name AS connectorName
            FROM station_connectors sc
            JOIN connectors c ON c.connector_id = sc.connector_id
            WHERE sc.station_id IN (:stationIds)
            ORDER BY sc.station_id, c.connector_name
            """, nativeQuery = true)
    List<StationConnectorView> findConnectorNamesByStationIds(@Param("stationIds") Collection<Long> stationIds);

    @Query(value = """
            SELECT c.connector_name AS connectorName,
                   sc.connector_count AS connectorCount,
                   sc.max_power_kw AS maxPowerKw
            FROM station_connectors sc
            JOIN connectors c ON c.connector_id = sc.connector_id
            WHERE sc.station_id = :stationId
            ORDER BY c.connector_name
            """, nativeQuery = true)
    List<StationConnectorDetailView> findConnectorDetailsByStationId(@Param("stationId") long stationId);

    interface StationConnectorView {

        Long getStationId();

        String getConnectorName();
    }

    interface StationConnectorDetailView {

        String getConnectorName();

        Integer getConnectorCount();

        BigDecimal getMaxPowerKw();
    }
}
