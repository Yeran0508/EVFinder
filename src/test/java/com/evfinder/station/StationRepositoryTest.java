package com.evfinder.station;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StationRepositoryTest {

    @Autowired
    private StationRepository stationRepository;

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

        List<Station> result = stationRepository.findByCityOrderByStationIdAsc("Repository Test City");

        assertThat(result)
                .extracting(Station::getStationName)
                .containsExactly("First Test Station", "Second Test Station");
    }
}
