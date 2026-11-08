package com.evfinder.station;

import java.math.BigDecimal;
import java.util.List;

public record StationResponse(
        Long id,
        String name,
        String address,
        String city,
        List<String> connectorTypes,
        BigDecimal maxPowerKw,
        BigDecimal averageRating,
        long reviewCount) {

    public StationResponse {
        connectorTypes = List.copyOf(connectorTypes);
    }
}
