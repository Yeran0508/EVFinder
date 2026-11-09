package com.evfinder.station;

import java.math.BigDecimal;

public record StationConnectorResponse(
        String type,
        Integer count,
        BigDecimal maxPowerKw) {
}
