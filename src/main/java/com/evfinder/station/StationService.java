package com.evfinder.station;

import java.math.BigDecimal;
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
    private final StationRatingCursorCodec ratingCursorCodec;

    public StationService(
            StationRepository stationRepository,
            ConnectorRepository connectorRepository,
            StationCursorCodec cursorCodec,
            StationRatingCursorCodec ratingCursorCodec) {
        this.stationRepository = stationRepository;
        this.connectorRepository = connectorRepository;
        this.cursorCodec = cursorCodec;
        this.ratingCursorCodec = ratingCursorCodec;
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
    public StationSearchResponse search(
            String city,
            String connectorType,
            String sort,
            String order,
            int limit,
            String cursor) {
        if (!"rating".equalsIgnoreCase(sort)) {
            throw new IllegalArgumentException("unsupported sort");
        }
        String normalizedOrder = order == null ? "desc" : order.trim().toLowerCase(Locale.ROOT);
        if (!normalizedOrder.equals("asc") && !normalizedOrder.equals("desc")) {
            throw new IllegalArgumentException("order must be asc or desc");
        }
        validateSearch(city, limit);

        String normalizedCity = city.trim();
        String normalizedConnectorType = normalizeConnectorType(connectorType);
        StationRatingCursorCodec.RatingCursor decodedCursor = ratingCursorCodec.decode(cursor, normalizedOrder);
        List<StationRepository.StationRatingView> ratingRows = stationRepository.findByCityOrderByRating(
                normalizedCity,
                normalizedConnectorType,
                normalizedOrder,
                decodedCursor.ratingSortValue(),
                decodedCursor.stationId(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = ratingRows.size() > limit;
        List<StationRepository.StationRatingView> pageRows = hasMore
                ? ratingRows.subList(0, limit)
                : ratingRows;
        List<Long> stationIds = pageRows.stream()
                .map(StationRepository.StationRatingView::getStationId)
                .toList();
        Map<Long, Station> stationsById = new HashMap<>();
        stationRepository.findAllById(stationIds)
                .forEach(station -> stationsById.put(station.getStationId(), station));
        Map<Long, List<String>> connectorTypesByStationId = loadConnectorTypes(
                stationIds.stream().map(stationsById::get).toList());

        List<StationResponse> items = pageRows.stream()
                .map(row -> toResponse(
                        stationsById.get(row.getStationId()),
                        connectorTypesByStationId.getOrDefault(row.getStationId(), List.of()),
                        row.getAverageRating(),
                        row.getReviewCount()))
                .toList();
        String nextCursor = null;
        if (hasMore) {
            StationRepository.StationRatingView last = pageRows.getLast();
            BigDecimal sortValue = last.getAverageRating() == null
                    ? new BigDecimal(normalizedOrder.equals("desc") ? "-1" : "6")
                    : last.getAverageRating();
            nextCursor = ratingCursorCodec.encode(normalizedOrder, sortValue, last.getStationId());
        }
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
        StationRepository.StationRatingSummary rating = stationRepository
                .findRatingSummaryByStationId(stationId);

        return new StationDetailResponse(
                station.getStationId(),
                station.getStationName(),
                formatAddress(station),
                station.getCity(),
                station.getState(),
                station.getLatitude(),
                station.getLongitude(),
                connectors,
                rating.getAverageRating(),
                rating.getReviewCount());
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
        return toResponse(station, connectorTypes, null, 0L);
    }

    private StationResponse toResponse(
            Station station,
            List<String> connectorTypes,
            BigDecimal averageRating,
            long reviewCount) {
        return new StationResponse(
                station.getStationId(),
                station.getStationName(),
                formatAddress(station),
                station.getCity(),
                connectorTypes,
                null,
                averageRating,
                reviewCount);
    }

    private void validateSearch(String city, int limit) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("city is required");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }

    private String formatAddress(Station station) {
        String zipCode = station.getZipCode() == null ? "" : " " + station.getZipCode();
        String cityAndState = "%s, %s%s".formatted(station.getCity(), station.getState(), zipCode);
        return station.getStreetAddress() == null
                ? cityAndState
                : "%s, %s".formatted(station.getStreetAddress(), cityAndState);
    }
}
