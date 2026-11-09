package com.evfinder.station.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @Test
    void preservesMissingStreetAddressAsUnknown(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("missing-address.csv");
        Files.writeString(csv, """
                ID,Station Name,Street Address,City,State,ZIP,Latitude,Longitude,Status Code,Access Code,Access Days Time,Facility Type,EV Level1 EVSE Num,EV Level2 EVSE Num,EV DC Fast Count,EV Pricing,EV Network,EV Network Web,EV Connector Types
                1,Address Unknown Station,,Chicago,IL,60601,41.881832,-87.623177,E,public,,,,1,,,Non-Networked,,J1772
                """);

        List<AfdcStationRecord> stations = reader.read(csv);

        assertEquals(1, stations.size());
        assertEquals(null, stations.getFirst().streetAddress());
    }
}
