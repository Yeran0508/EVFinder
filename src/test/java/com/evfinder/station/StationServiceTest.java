package com.evfinder.station;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class StationServiceTest {

    private final StationRepository stationRepository = mock(StationRepository.class);
    private final StationService stationService = new StationService(stationRepository);

    @Test
    void searchesByTrimmedCityAndMapsResponse() {
        Station station = mock(Station.class);
        when(station.getStationId()).thenReturn(10L);
        when(station.getStationName()).thenReturn("Test Charging Station");
        when(station.getStreetAddress()).thenReturn("100 Main Street");
        when(station.getCity()).thenReturn("Chicago");
        when(station.getState()).thenReturn("IL");
        when(station.getZipCode()).thenReturn("60601");
        when(stationRepository.findByCityOrderByStationIdAsc("Chicago")).thenReturn(List.of(station));

        List<StationResponse> result = stationService.searchByCity("  Chicago  ");

        assertThat(result).containsExactly(new StationResponse(
                10L,
                "Test Charging Station",
                "100 Main Street, Chicago, IL 60601",
                "Chicago",
                List.of(),
                null,
                null,
                0));
        verify(stationRepository).findByCityOrderByStationIdAsc("Chicago");
    }

    @Test
    void rejectsBlankCityWithoutQueryingRepository() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> stationService.searchByCity("   "))
                .withMessage("city is required");

        verifyNoInteractions(stationRepository);
    }
}
