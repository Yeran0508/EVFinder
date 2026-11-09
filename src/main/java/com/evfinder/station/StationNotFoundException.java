package com.evfinder.station;

public class StationNotFoundException extends RuntimeException {

    public StationNotFoundException(long stationId) {
        super("station " + stationId + " was not found");
    }
}
