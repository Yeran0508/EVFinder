package com.evfinder.station;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StationRepositoryTest {

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ConnectorRepository connectorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsStationsByCityOrderedById() {
        jdbcTemplate.update("""
                INSERT INTO charging_networks (network_name)
                VALUES ('Repository Test Network') AS incoming
                ON DUPLICATE KEY UPDATE network_name = incoming.network_name
                """);
        Long networkId = jdbcTemplate.queryForObject(
                "SELECT network_id FROM charging_networks WHERE network_name = 'Repository Test Network'",
                Long.class);

        Station first = new Station(
                "repository-test-1", networkId, "First Test Station", "1 Test Street",
                "Repository Test City", "IL", "60001",
                new BigDecimal("41.100000"), new BigDecimal("-87.100000"));
        Station second = new Station(
                "repository-test-2", networkId, "Second Test Station", "2 Test Street",
                "Repository Test City", "IL", "60002",
                new BigDecimal("41.200000"), new BigDecimal("-87.200000"));
        stationRepository.saveAllAndFlush(List.of(first, second));

        jdbcTemplate.update("""
                INSERT INTO connectors (connector_name, afdc_code)
                VALUES ('Repository Test Connector', 'REPOSITORY_TEST') AS incoming
                ON DUPLICATE KEY UPDATE afdc_code = incoming.afdc_code
                """);
        Long connectorId = jdbcTemplate.queryForObject(
                "SELECT connector_id FROM connectors WHERE connector_name = 'Repository Test Connector'",
                Long.class);
        jdbcTemplate.update(
                "INSERT INTO station_connectors (station_id, connector_id) VALUES (?, ?)",
                first.getStationId(), connectorId);

        List<Station> result = stationRepository.findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                "Repository Test City", 0, PageRequest.of(0, 20));
        List<Station> limited = stationRepository.findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                "Repository Test City", 0, PageRequest.of(0, 1));
        List<Station> afterFirst = stationRepository.findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                "Repository Test City", first.getStationId(), PageRequest.of(0, 20));
        List<Station> filtered = stationRepository.findByCityAndConnectorTypeAfterStationId(
                "Repository Test City", "Repository Test Connector", 0, PageRequest.of(0, 20));
        List<ConnectorRepository.StationConnectorView> connectors =
                connectorRepository.findConnectorNamesByStationIds(
                        List.of(first.getStationId(), second.getStationId()));
        List<ConnectorRepository.StationConnectorDetailView> connectorDetails =
                connectorRepository.findConnectorDetailsByStationId(first.getStationId());

        assertThat(result)
                .extracting(Station::getStationName)
                .containsExactly("First Test Station", "Second Test Station");
        assertThat(limited)
                .extracting(Station::getStationName)
                .containsExactly("First Test Station");
        assertThat(afterFirst)
                .extracting(Station::getStationName)
                .containsExactly("Second Test Station");
        assertThat(filtered)
                .extracting(Station::getStationName)
                .containsExactly("First Test Station");
        assertThat(connectors)
                .extracting(ConnectorRepository.StationConnectorView::getConnectorName)
                .containsExactly("Repository Test Connector");
        assertThat(connectorDetails)
                .extracting(ConnectorRepository.StationConnectorDetailView::getConnectorName)
                .containsExactly("Repository Test Connector");
    }
}
