package com.evfinder.station.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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

    @Test
    void aggregatesChargingUnitRowsByStation(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("charging-units.csv");
        Files.writeString(csv, """
                ID,Station Name,Street Address,City,State,ZIP,Latitude,Longitude,Status Code,Access Code,Access Days Time,Facility Type,EV Level1 EVSE Num,EV Level2 EVSE Num,EV DC Fast Count,EV Pricing,EV Network,EV Network Web,EV Connector Types,EV J1772 Connector Count,EV J1772 Power Output (kW),EV CCS Connector Count,EV CCS Power Output (kW),EV CHAdeMO Connector Count,EV CHAdeMO Power Output (kW),EV J3400 Connector Count,EV J3400 Power Output (kW),EV J3271 Connector Count,EV J3271 Power Output (kW)
                10,Multi Unit Station,1 Main St,Chicago,IL,60601,41.100000,-87.100000,E,public,,,,2,2,,Network,,J1772 J1772COMBO,1,7.2,1,150.0,0,,0,,0,
                10,Multi Unit Station,1 Main St,Chicago,IL,60601,41.100000,-87.100000,E,public,,,,2,2,,Network,,J1772 J1772COMBO,1,11.5,1,350.0,0,,0,,0,
                """);

        AfdcStationCsvReader.ReadResult result = reader.readWithMetadata(csv);
        AfdcStationRecord station = result.stations().getFirst();
        ConnectorTypeNormalizer.NormalizedConnector j1772 = station.connectors().stream()
                .filter(connector -> connector.name().equals("J1772"))
                .findFirst()
                .orElseThrow();
        ConnectorTypeNormalizer.NormalizedConnector ccs = station.connectors().stream()
                .filter(connector -> connector.name().equals("CCS"))
                .findFirst()
                .orElseThrow();

        assertEquals(2, result.sourceRows());
        assertEquals(1, result.stations().size());
        assertEquals(2, j1772.count());
        assertEquals(new BigDecimal("11.5"), j1772.maxPowerKw());
        assertEquals(2, ccs.count());
        assertEquals(new BigDecimal("350.0"), ccs.maxPowerKw());
    }
}
