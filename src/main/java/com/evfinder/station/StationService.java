package com.evfinder.station;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private static final Map<String, String> CONNECTOR_TYPES = Map.of(
            "ccs", "CCS",
            "chademo", "CHAdeMO",
            "j1772", "J1772");

    private final StationRepository stationRepository;
    private final ConnectorRepository connectorRepository;
    private final StationCursorCodec cursorCodec;

    public StationService(
            StationRepository stationRepository,
            ConnectorRepository connectorRepository,
            StationCursorCodec cursorCodec) {
        this.stationRepository = stationRepository;
        this.connectorRepository = connectorRepository;
        this.cursorCodec = cursorCodec;
    }

    @Transactional(readOnly = true)
    public List<StationResponse> searchByCity(String city) {
        return searchByCity(city, null, DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<StationResponse> searchByCity(String city, String connectorType) {
        return searchByCity(city, connectorType, DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<StationResponse> searchByCity(String city, String connectorType, int limit) {
        return search(city, connectorType, limit, null).items();
    }

    @Transactional(readOnly = true)
    public StationSearchResponse search(String city, String connectorType, int limit, String cursor) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("city is required");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }

        String normalizedCity = city.trim();
        String normalizedConnectorType = normalizeConnectorType(connectorType);
        long afterStationId = cursorCodec.decode(cursor);
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Station> stations = normalizedConnectorType == null
                ? stationRepository.findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                        normalizedCity, afterStationId, pageable)
                : stationRepository.findByCityAndConnectorTypeAfterStationId(
                        normalizedCity, normalizedConnectorType, afterStationId, pageable);

        boolean hasMore = stations.size() > limit;
        List<Station> pageStations = hasMore ? stations.subList(0, limit) : stations;
        Map<Long, List<String>> connectorTypesByStationId = loadConnectorTypes(pageStations);
        List<StationResponse> items = pageStations.stream()
                .map(station -> toResponse(
                        station,
                        connectorTypesByStationId.getOrDefault(station.getStationId(), List.of())))
                .toList();
        String nextCursor = hasMore
                ? cursorCodec.encode(pageStations.getLast().getStationId())
                : null;
        return new StationSearchResponse(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public StationDetailResponse getStation(long stationId) {
        if (stationId <= 0) {
            throw new IllegalArgumentException("stationId must be positive");
        }

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new StationNotFoundException(stationId));
        List<StationConnectorResponse> connectors = connectorRepository
                .findConnectorDetailsByStationId(stationId)
                .stream()
                .map(connector -> new StationConnectorResponse(
                        connector.getConnectorName(),
                        connector.getConnectorCount(),
                        connector.getMaxPowerKw()))
                .toList();

        return new StationDetailResponse(
                station.getStationId(),
                station.getStationName(),
                formatAddress(station),
                station.getCity(),
                station.getState(),
                station.getLatitude(),
                station.getLongitude(),
                connectors,
                null,
                0);
    }

    private String normalizeConnectorType(String connectorType) {
        if (connectorType == null || connectorType.isBlank()) {
            return null;
        }

        String normalized = CONNECTOR_TYPES.get(connectorType.trim().toLowerCase(Locale.ROOT));
        if (normalized == null) {
            throw new IllegalArgumentException("unsupported connectorType");
        }
        return normalized;
    }

    private Map<Long, List<String>> loadConnectorTypes(List<Station> stations) {
        if (stations.isEmpty()) {
            return Map.of();
        }

        List<Long> stationIds = stations.stream()
                .map(Station::getStationId)
                .toList();
        Map<Long, List<String>> connectorTypesByStationId = new HashMap<>();
        for (ConnectorRepository.StationConnectorView connector
                : connectorRepository.findConnectorNamesByStationIds(stationIds)) {
            connectorTypesByStationId
                    .computeIfAbsent(connector.getStationId(), ignored -> new ArrayList<>())
                    .add(connector.getConnectorName());
        }
        return connectorTypesByStationId;
    }

    private StationResponse toResponse(Station station, List<String> connectorTypes) {
        return new StationResponse(
                station.getStationId(),
                station.getStationName(),
                formatAddress(station),
                station.getCity(),
                connectorTypes,
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
