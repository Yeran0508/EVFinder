package com.evfinder.station;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class StationServiceTest {

    private final StationRepository stationRepository = mock(StationRepository.class);
    private final ConnectorRepository connectorRepository = mock(ConnectorRepository.class);
    private final StationCursorCodec cursorCodec = new StationCursorCodec();
    private final StationRatingCursorCodec ratingCursorCodec = new StationRatingCursorCodec();
    private final StationPowerCursorCodec powerCursorCodec = new StationPowerCursorCodec();
    private final StationService stationService = new StationService(
            stationRepository, connectorRepository, cursorCodec, ratingCursorCodec, powerCursorCodec);

    @Test
    void searchesByTrimmedCityAndMapsResponse() {
        Station station = mock(Station.class);
        when(station.getStationId()).thenReturn(10L);
        when(station.getStationName()).thenReturn("Test Charging Station");
        when(station.getStreetAddress()).thenReturn("100 Main Street");
        when(station.getCity()).thenReturn("Chicago");
        when(station.getState()).thenReturn("IL");
        when(station.getZipCode()).thenReturn("60601");
        when(stationRepository.findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                "Chicago", 0, PageRequest.of(0, 21)))
                .thenReturn(List.of(station));
        ConnectorRepository.StationConnectorView connector = mock(ConnectorRepository.StationConnectorView.class);
        when(connector.getStationId()).thenReturn(10L);
        when(connector.getConnectorName()).thenReturn("CCS");
        when(connectorRepository.findConnectorNamesByStationIds(List.of(10L))).thenReturn(List.of(connector));

        List<StationResponse> result = stationService.searchByCity("  Chicago  ");

        assertThat(result).containsExactly(new StationResponse(
                10L,
                "Test Charging Station",
                "100 Main Street, Chicago, IL 60601",
                "Chicago",
                List.of("CCS"),
                null,
                null,
                0));
        verify(stationRepository).findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                "Chicago", 0, PageRequest.of(0, 21));
        verify(connectorRepository).findConnectorNamesByStationIds(List.of(10L));
    }

    @Test
    void filtersByNormalizedConnectorType() {
        when(stationRepository.findByCityAndConnectorTypeAfterStationId(
                "Chicago", "CCS", 0, PageRequest.of(0, 21)))
                .thenReturn(List.of());

        List<StationResponse> result = stationService.searchByCity("Chicago", " ccs ");

        assertThat(result).isEmpty();
        verify(stationRepository).findByCityAndConnectorTypeAfterStationId(
                "Chicago", "CCS", 0, PageRequest.of(0, 21));
        verifyNoInteractions(connectorRepository);
    }

    @Test
    void passesLimitToRepository() {
        when(stationRepository.findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                "Chicago", 0, PageRequest.of(0, 2)))
                .thenReturn(List.of());

        List<StationResponse> result = stationService.searchByCity("Chicago", null, 1);

        assertThat(result).isEmpty();
        verify(stationRepository).findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                "Chicago", 0, PageRequest.of(0, 2));
    }

    @Test
    void returnsNextCursorWhenMoreStationsExist() {
        Station first = mock(Station.class);
        Station second = mock(Station.class);
        when(first.getStationId()).thenReturn(10L);
        when(first.getStationName()).thenReturn("First");
        when(first.getStreetAddress()).thenReturn("1 Main Street");
        when(first.getCity()).thenReturn("Chicago");
        when(first.getState()).thenReturn("IL");
        when(second.getStationId()).thenReturn(11L);
        when(stationRepository.findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                "Chicago", 0, PageRequest.of(0, 2)))
                .thenReturn(List.of(first, second));
        when(connectorRepository.findConnectorNamesByStationIds(List.of(10L))).thenReturn(List.of());

        StationSearchResponse result = stationService.search("Chicago", null, 1, null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.nextCursor()).isEqualTo(cursorCodec.encode(10L));
    }

    @Test
    void decodesCursorBeforeQueryingRepository() {
        String cursor = cursorCodec.encode(10L);
        when(stationRepository.findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                "Chicago", 10L, PageRequest.of(0, 2)))
                .thenReturn(List.of());

        StationSearchResponse result = stationService.search("Chicago", null, 1, cursor);

        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
        verify(stationRepository).findByCityAndStationIdGreaterThanOrderByStationIdAsc(
                "Chicago", 10L, PageRequest.of(0, 2));
    }

    @Test
    void rejectsInvalidCursor() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> stationService.search("Chicago", null, 20, "not-a-cursor"))
                .withMessage("invalid cursor");

        verifyNoInteractions(stationRepository, connectorRepository);
    }

    @Test
    void rejectsLimitOutsideAllowedRange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> stationService.searchByCity("Chicago", null, 0))
                .withMessage("limit must be between 1 and 100");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> stationService.searchByCity("Chicago", null, 101))
                .withMessage("limit must be between 1 and 100");

        verifyNoInteractions(stationRepository, connectorRepository);
    }

    @Test
    void rejectsUnsupportedConnectorType() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> stationService.searchByCity("Chicago", "Type2"))
                .withMessage("unsupported connectorType");

        verifyNoInteractions(stationRepository, connectorRepository);
    }

    @Test
    void rejectsBlankCityWithoutQueryingRepository() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> stationService.searchByCity("   "))
                .withMessage("city is required");

        verifyNoInteractions(stationRepository, connectorRepository);
    }

    @Test
    void returnsStationDetailsWithConnectors() {
        Station station = mock(Station.class);
        when(station.getStationId()).thenReturn(10L);
        when(station.getStationName()).thenReturn("Test Charging Station");
        when(station.getStreetAddress()).thenReturn("100 Main Street");
        when(station.getCity()).thenReturn("Chicago");
        when(station.getState()).thenReturn("IL");
        when(station.getZipCode()).thenReturn("60601");
        when(station.getLatitude()).thenReturn(new BigDecimal("41.100000"));
        when(station.getLongitude()).thenReturn(new BigDecimal("-87.100000"));
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));

        ConnectorRepository.StationConnectorDetailView connector =
                mock(ConnectorRepository.StationConnectorDetailView.class);
        when(connector.getConnectorName()).thenReturn("CCS");
        when(connector.getConnectorCount()).thenReturn(2);
        when(connector.getMaxPowerKw()).thenReturn(new BigDecimal("150.00"));
        when(connectorRepository.findConnectorDetailsByStationId(10L)).thenReturn(List.of(connector));
        StationRepository.StationRatingSummary rating = mock(StationRepository.StationRatingSummary.class);
        when(rating.getAverageRating()).thenReturn(new BigDecimal("4.50"));
        when(rating.getReviewCount()).thenReturn(2L);
        when(stationRepository.findRatingSummaryByStationId(10L)).thenReturn(rating);

        StationDetailResponse result = stationService.getStation(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.address()).isEqualTo("100 Main Street, Chicago, IL 60601");
        assertThat(result.connectors()).containsExactly(
                new StationConnectorResponse("CCS", 2, new BigDecimal("150.00")));
        assertThat(result.averageRating()).isEqualByComparingTo("4.50");
        assertThat(result.reviewCount()).isEqualTo(2);
    }

    @Test
    void formatsUnknownStreetAddressWithoutLiteralNull() {
        Station station = mock(Station.class);
        when(station.getStationId()).thenReturn(11L);
        when(station.getStationName()).thenReturn("Address Unknown Station");
        when(station.getCity()).thenReturn("Chicago");
        when(station.getState()).thenReturn("IL");
        when(station.getZipCode()).thenReturn("60601");
        when(station.getLatitude()).thenReturn(new BigDecimal("41.100000"));
        when(station.getLongitude()).thenReturn(new BigDecimal("-87.100000"));
        when(stationRepository.findById(11L)).thenReturn(Optional.of(station));
        when(connectorRepository.findConnectorDetailsByStationId(11L)).thenReturn(List.of());
        StationRepository.StationRatingSummary rating = mock(StationRepository.StationRatingSummary.class);
        when(rating.getReviewCount()).thenReturn(0L);
        when(stationRepository.findRatingSummaryByStationId(11L)).thenReturn(rating);

        StationDetailResponse result = stationService.getStation(11L);

        assertThat(result.address()).isEqualTo("Chicago, IL 60601");
    }

    @Test
    void sortsByRatingAndReturnsRatingCursor() {
        StationRepository.StationRatingView first = mock(StationRepository.StationRatingView.class);
        StationRepository.StationRatingView second = mock(StationRepository.StationRatingView.class);
        when(first.getStationId()).thenReturn(10L);
        when(first.getAverageRating()).thenReturn(new BigDecimal("4.50"));
        when(first.getReviewCount()).thenReturn(2L);
        when(second.getStationId()).thenReturn(11L);
        when(second.getAverageRating()).thenReturn(new BigDecimal("4.00"));
        when(second.getReviewCount()).thenReturn(1L);
        when(stationRepository.findByCityOrderByRating(
                "Chicago", null, "desc", new BigDecimal("6"), 0, PageRequest.of(0, 2)))
                .thenReturn(List.of(first, second));
        Station station = mock(Station.class);
        when(station.getStationId()).thenReturn(10L);
        when(station.getStationName()).thenReturn("Rated Station");
        when(station.getStreetAddress()).thenReturn("1 Main Street");
        when(station.getCity()).thenReturn("Chicago");
        when(station.getState()).thenReturn("IL");
        when(stationRepository.findAllById(List.of(10L))).thenReturn(List.of(station));
        when(connectorRepository.findConnectorNamesByStationIds(List.of(10L))).thenReturn(List.of());
        StationRepository.StationMetricsView metrics = mock(StationRepository.StationMetricsView.class);
        when(metrics.getStationId()).thenReturn(10L);
        when(metrics.getAverageRating()).thenReturn(new BigDecimal("4.50"));
        when(metrics.getReviewCount()).thenReturn(2L);
        when(stationRepository.findMetricsByStationIds(List.of(10L))).thenReturn(List.of(metrics));

        StationSearchResponse result = stationService.search(
                "Chicago", null, "rating", "desc", 1, null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().averageRating()).isEqualByComparingTo("4.50");
        assertThat(result.items().getFirst().reviewCount()).isEqualTo(2);
        assertThat(result.nextCursor()).isEqualTo(
                ratingCursorCodec.encode("desc", new BigDecimal("4.50"), 10L));
    }

    @Test
    void rejectsUnsupportedSortOrder() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> stationService.search(
                        "Chicago", null, "rating", "sideways", 20, null))
                .withMessage("order must be asc or desc");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> stationService.search(
                        "Chicago", null, "distance", "desc", 20, null))
                .withMessage("unsupported sort");
    }

    @Test
    void sortsByPowerAndReturnsPowerCursor() {
        StationRepository.StationPowerView first = mock(StationRepository.StationPowerView.class);
        StationRepository.StationPowerView second = mock(StationRepository.StationPowerView.class);
        when(first.getStationId()).thenReturn(10L);
        when(first.getMaxPowerKw()).thenReturn(new BigDecimal("350.00"));
        when(second.getStationId()).thenReturn(11L);
        when(second.getMaxPowerKw()).thenReturn(new BigDecimal("150.00"));
        when(stationRepository.findByCityOrderByPower(
                "Chicago", "CCS", "desc", BigDecimal.ZERO, 0, PageRequest.of(0, 2)))
                .thenReturn(List.of(first, second));
        Station station = mock(Station.class);
        when(station.getStationId()).thenReturn(10L);
        when(station.getStationName()).thenReturn("Fast Station");
        when(station.getStreetAddress()).thenReturn("1 Main Street");
        when(station.getCity()).thenReturn("Chicago");
        when(station.getState()).thenReturn("IL");
        when(stationRepository.findAllById(List.of(10L))).thenReturn(List.of(station));
        when(connectorRepository.findConnectorNamesByStationIds(List.of(10L))).thenReturn(List.of());
        StationRepository.StationMetricsView metrics = mock(StationRepository.StationMetricsView.class);
        when(metrics.getStationId()).thenReturn(10L);
        when(metrics.getMaxPowerKw()).thenReturn(new BigDecimal("350.00"));
        when(stationRepository.findMetricsByStationIds(List.of(10L))).thenReturn(List.of(metrics));

        StationSearchResponse result = stationService.search(
                "Chicago", "CCS", "power", "desc", 1, null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().maxPowerKw()).isEqualByComparingTo("350.00");
        assertThat(result.nextCursor()).isEqualTo(
                powerCursorCodec.encode("desc", new BigDecimal("350.00"), 10L));
    }

    @Test
    void rejectsNonPositiveStationId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> stationService.getStation(0))
                .withMessage("stationId must be positive");

        verifyNoInteractions(stationRepository, connectorRepository);
    }

    @Test
    void reportsMissingStation() {
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.getStation(999L))
                .isInstanceOf(StationNotFoundException.class)
                .hasMessage("station 999 was not found");

        verifyNoInteractions(connectorRepository);
    }
}
