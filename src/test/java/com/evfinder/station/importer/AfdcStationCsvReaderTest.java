package com.evfinder.station.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class AfdcStationCsvReaderTest {

    private final AfdcStationCsvReader reader = new AfdcStationCsvReader();

    @Test
    void readsFixedAfdcFixture() throws Exception {
        Path fixture = Path.of(getClass().getResource("/fixtures/afdc-stations-sample.csv").toURI());

        List<AfdcStationRecord> stations = reader.read(fixture);

        assertEquals(15, stations.size());
        assertEquals("40794", stations.getFirst().externalId());
        assertEquals("Chicago", stations.getFirst().city());
        assertTrue(stations.stream().anyMatch(station ->
                station.connectors().stream().anyMatch(connector -> connector.name().equals("CCS"))));
        assertTrue(stations.stream().anyMatch(station ->
                station.connectors().stream().anyMatch(connector -> connector.name().equals("NACS"))));
    }
}
