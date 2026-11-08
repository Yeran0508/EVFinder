package com.evfinder.station;

import java.util.List;

public record StationSearchResponse(
        List<StationResponse> items,
        String nextCursor) {

    public StationSearchResponse {
        items = List.copyOf(items);
    }
}
