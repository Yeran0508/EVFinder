package com.evfinder.station.importer;

import java.math.BigDecimal;
import java.util.List;

public record AfdcStationRecord(
        String externalId,
        String stationName,
        String streetAddress,
        String city,
        String state,
        String zipCode,
        BigDecimal latitude,
        BigDecimal longitude,
        String statusCode,
        String accessCode,
        String accessDaysTime,
        String facilityType,
        int level1EvseCount,
        int level2EvseCount,
        int dcFastCount,
        String pricingText,
        String networkName,
        String networkUrl,
        List<ConnectorTypeNormalizer.NormalizedConnector> connectors) {
}
