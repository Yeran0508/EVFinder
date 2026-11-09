package com.evfinder.station;

import java.math.BigDecimal;
import java.util.List;

public record StationDetailResponse(
        Long id,
        String name,
        String address,
        String city,
        String state,
        BigDecimal latitude,
        BigDecimal longitude,
        List<StationConnectorResponse> connectors,
        BigDecimal averageRating,
        long reviewCount) {

    public StationDetailResponse {
        connectors = List.copyOf(connectors);
    }
}
