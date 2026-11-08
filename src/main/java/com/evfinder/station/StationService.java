package com.evfinder.station;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public List<StationResponse> searchByCity(String city) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("city is required");
        }

        String normalizedCity = city.trim();
        return stationRepository.findByCityOrderByStationIdAsc(normalizedCity).stream()
                .map(this::toResponse)
                .toList();
    }

    private StationResponse toResponse(Station station) {
        return new StationResponse(
                station.getStationId(),
                station.getStationName(),
                formatAddress(station),
                station.getCity(),
                List.of(),
                null,
                null,
                0);
    }

    private String formatAddress(Station station) {
        String zipCode = station.getZipCode() == null ? "" : " " + station.getZipCode();
        return "%s, %s, %s%s".formatted(
                station.getStreetAddress(),
                station.getCity(),
                station.getState(),
                zipCode);
    }
}
