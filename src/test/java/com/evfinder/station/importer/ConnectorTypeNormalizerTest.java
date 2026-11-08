package com.evfinder.station.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class ConnectorTypeNormalizerTest {

    @Test
    void normalizesMultipleAfdcConnectorCodes() {
        List<String> names = ConnectorTypeNormalizer.normalize("CHADEMO J1772 J1772COMBO TESLA")
                .stream()
                .map(ConnectorTypeNormalizer.NormalizedConnector::name)
                .toList();

        assertEquals(List.of("CHAdeMO", "J1772", "CCS", "NACS"), names);
    }

    @Test
    void rejectsUnknownConnectorCode() {
        assertThrows(IllegalArgumentException.class,
                () -> ConnectorTypeNormalizer.normalize("UNKNOWN"));
    }
}
