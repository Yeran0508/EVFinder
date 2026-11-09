package com.evfinder.station.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AfdcStationImportService {

    private final AfdcStationCsvReader csvReader;
    private final JdbcTemplate jdbcTemplate;

    public AfdcStationImportService(AfdcStationCsvReader csvReader, JdbcTemplate jdbcTemplate) {
        this.csvReader = csvReader;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ImportResult importFile(Path path) throws IOException {
        AfdcStationCsvReader.ReadResult readResult = csvReader.readWithMetadata(path);
        List<AfdcStationRecord> records = readResult.stations();
        for (AfdcStationRecord record : records) {
            importStation(record);
        }
        return new ImportResult(readResult.sourceRows(), records.size());
    }

    private void importStation(AfdcStationRecord station) {
        upsertNetwork(station.networkName(), station.networkUrl());
        long networkId = jdbcTemplate.queryForObject(
                "SELECT network_id FROM charging_networks WHERE network_name = ?",
                Long.class,
                station.networkName());

        jdbcTemplate.update("""
                INSERT INTO charging_stations (
                    external_id, network_id, station_name, street_address, city, state, zip_code,
                    latitude, longitude, status_code, access_code, access_days_time, facility_type,
                    level1_evse_count, level2_evse_count, dc_fast_count, pricing_text
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AS incoming
                ON DUPLICATE KEY UPDATE
                    network_id = incoming.network_id,
                    station_name = incoming.station_name,
                    street_address = incoming.street_address,
                    city = incoming.city,
                    state = incoming.state,
                    zip_code = incoming.zip_code,
                    latitude = incoming.latitude,
                    longitude = incoming.longitude,
                    status_code = incoming.status_code,
                    access_code = incoming.access_code,
                    access_days_time = incoming.access_days_time,
                    facility_type = incoming.facility_type,
                    level1_evse_count = incoming.level1_evse_count,
                    level2_evse_count = incoming.level2_evse_count,
                    dc_fast_count = incoming.dc_fast_count,
                    pricing_text = incoming.pricing_text
                """,
                station.externalId(), networkId, station.stationName(), station.streetAddress(), station.city(),
                station.state(), station.zipCode(), station.latitude(), station.longitude(), station.statusCode(),
                station.accessCode(), station.accessDaysTime(), station.facilityType(), station.level1EvseCount(),
                station.level2EvseCount(), station.dcFastCount(), station.pricingText());

        long stationId = jdbcTemplate.queryForObject(
                "SELECT station_id FROM charging_stations WHERE external_id = ?",
                Long.class,
                station.externalId());

        jdbcTemplate.update("DELETE FROM station_connectors WHERE station_id = ?", stationId);
        for (ConnectorTypeNormalizer.NormalizedConnector connector : station.connectors()) {
            upsertConnector(connector);
            long connectorId = jdbcTemplate.queryForObject(
                    "SELECT connector_id FROM connectors WHERE connector_name = ?",
                    Long.class,
                    connector.name());
            jdbcTemplate.update(
                    "INSERT INTO station_connectors "
                            + "(station_id, connector_id, connector_count, max_power_kw) VALUES (?, ?, ?, ?)",
                    new Object[] {stationId, connectorId, connector.count(), connector.maxPowerKw()},
                    new int[] {Types.BIGINT, Types.BIGINT, Types.INTEGER, Types.DECIMAL});
        }
    }

    private void upsertNetwork(String networkName, String networkUrl) {
        jdbcTemplate.update("""
                INSERT INTO charging_networks (network_name, network_url)
                VALUES (?, ?) AS incoming
                ON DUPLICATE KEY UPDATE
                    network_url = COALESCE(incoming.network_url, charging_networks.network_url)
                """, networkName, networkUrl);
    }

    private void upsertConnector(ConnectorTypeNormalizer.NormalizedConnector connector) {
        jdbcTemplate.update("""
                INSERT INTO connectors (connector_name, afdc_code)
                VALUES (?, ?) AS incoming
                ON DUPLICATE KEY UPDATE
                    afdc_code = incoming.afdc_code
                """, connector.name(), connector.afdcCode());
    }

    public record ImportResult(int totalRows, int importedStations) {
    }
}
