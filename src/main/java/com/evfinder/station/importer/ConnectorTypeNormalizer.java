package com.evfinder.station.importer;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConnectorTypeNormalizer {

    private static final Map<String, String> CONNECTOR_NAMES = Map.of(
            "J1772", "J1772",
            "J1772COMBO", "CCS",
            "CHADEMO", "CHAdeMO",
            "TESLA", "NACS",
            "J3271", "MCS",
            "NEMA1450", "NEMA 14-50",
            "NEMA515", "NEMA 5-15",
            "NEMA520", "NEMA 5-20");

    private ConnectorTypeNormalizer() {
    }

    public static List<NormalizedConnector> normalize(String sourceValue) {
        if (sourceValue == null || sourceValue.isBlank()) {
            throw new IllegalArgumentException("EV Connector Types is required");
        }

        Map<String, NormalizedConnector> connectors = new LinkedHashMap<>();
        Arrays.stream(sourceValue.trim().split("\\s+"))
                .map(String::toUpperCase)
                .forEach(code -> {
                    String name = CONNECTOR_NAMES.get(code);
                    if (name == null) {
                        throw new IllegalArgumentException("Unsupported connector code: " + code);
                    }
                    connectors.putIfAbsent(name, new NormalizedConnector(name, code, null, null));
                });
        return List.copyOf(connectors.values());
    }

    public record NormalizedConnector(
            String name,
            String afdcCode,
            Integer count,
            BigDecimal maxPowerKw) {
    }
}
