package com.evfinder.station;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StationController.class)
class StationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationService stationService;

    @Test
    void returnsStationsForCity() throws Exception {
        StationResponse station = new StationResponse(
                1L,
                "Test Charging Station",
                "100 Main Street, Chicago, IL 60601",
                "Chicago",
                List.of("CCS"),
                null,
                null,
                0);
        when(stationService.search("Chicago", null, 20, null))
                .thenReturn(new StationSearchResponse(List.of(station), null));

        mockMvc.perform(get("/api/v1/stations").param("city", "Chicago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Test Charging Station"))
                .andExpect(jsonPath("$.items[0].connectorTypes[0]").value("CCS"))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));

        verify(stationService).search("Chicago", null, 20, null);
    }

    @Test
    void returnsEmptyItemsWhenNoStationsMatch() throws Exception {
        when(stationService.search("Unknown City", null, 20, null))
                .thenReturn(new StationSearchResponse(List.of(), null));

        mockMvc.perform(get("/api/v1/stations").param("city", "Unknown City"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    @Test
    void returnsBadRequestWhenCityIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stationService);
    }

    @Test
    void returnsBadRequestWhenCityIsBlank() throws Exception {
        when(stationService.search("   ", null, 20, null))
                .thenThrow(new IllegalArgumentException("city is required"));

        mockMvc.perform(get("/api/v1/stations").param("city", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("city is required"));

        verify(stationService).search("   ", null, 20, null);
    }

    @Test
    void passesConnectorTypeToService() throws Exception {
        when(stationService.search("Chicago", "CCS", 20, null))
                .thenReturn(new StationSearchResponse(List.of(), null));

        mockMvc.perform(get("/api/v1/stations")
                        .param("city", "Chicago")
                        .param("connectorType", "CCS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(stationService).search("Chicago", "CCS", 20, null);
    }

    @Test
    void returnsBadRequestForUnsupportedConnectorType() throws Exception {
        when(stationService.search("Chicago", "Type2", 20, null))
                .thenThrow(new IllegalArgumentException("unsupported connectorType"));

        mockMvc.perform(get("/api/v1/stations")
                        .param("city", "Chicago")
                        .param("connectorType", "Type2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported connectorType"));

        verify(stationService).search("Chicago", "Type2", 20, null);
    }

    @Test
    void passesCustomLimitToService() throws Exception {
        when(stationService.search("Chicago", "CCS", 1, null))
                .thenReturn(new StationSearchResponse(List.of(), null));

        mockMvc.perform(get("/api/v1/stations")
                        .param("city", "Chicago")
                        .param("connectorType", "CCS")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(stationService).search("Chicago", "CCS", 1, null);
    }

    @Test
    void returnsBadRequestWhenLimitIsOutsideAllowedRange() throws Exception {
        when(stationService.search("Chicago", null, 0, null))
                .thenThrow(new IllegalArgumentException("limit must be between 1 and 100"));
        when(stationService.search("Chicago", null, 101, null))
                .thenThrow(new IllegalArgumentException("limit must be between 1 and 100"));

        mockMvc.perform(get("/api/v1/stations").param("city", "Chicago").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("limit must be between 1 and 100"));
        mockMvc.perform(get("/api/v1/stations").param("city", "Chicago").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("limit must be between 1 and 100"));
    }

    @Test
    void passesCursorToServiceAndReturnsNextCursor() throws Exception {
        when(stationService.search("Chicago", null, 1, "MQ"))
                .thenReturn(new StationSearchResponse(List.of(), "Mg"));

        mockMvc.perform(get("/api/v1/stations")
                        .param("city", "Chicago")
                        .param("limit", "1")
                        .param("cursor", "MQ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").value("Mg"));

        verify(stationService).search("Chicago", null, 1, "MQ");
    }

    @Test
    void passesRatingSortAndOrderToService() throws Exception {
        when(stationService.search("Chicago", "CCS", "rating", "asc", 20, null))
                .thenReturn(new StationSearchResponse(List.of(), null));

        mockMvc.perform(get("/api/v1/stations")
                        .param("city", "Chicago")
                        .param("connectorType", "CCS")
                        .param("sort", "rating")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(stationService).search("Chicago", "CCS", "rating", "asc", 20, null);
    }

    @Test
    void returnsBadRequestForInvalidCursor() throws Exception {
        when(stationService.search("Chicago", null, 20, "invalid"))
                .thenThrow(new IllegalArgumentException("invalid cursor"));

        mockMvc.perform(get("/api/v1/stations")
                        .param("city", "Chicago")
                        .param("cursor", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid cursor"));
    }

    @Test
    void returnsStationDetails() throws Exception {
        StationDetailResponse detail = new StationDetailResponse(
                1L,
                "Detail Station",
                "100 Main Street, Chicago, IL 60601",
                "Chicago",
                "IL",
                new BigDecimal("41.100000"),
                new BigDecimal("-87.100000"),
                List.of(new StationConnectorResponse("CCS", null, null)),
                null,
                0);
        when(stationService.getStation(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/stations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.city").value("Chicago"))
                .andExpect(jsonPath("$.connectors[0].type").value("CCS"));

        verify(stationService).getStation(1L);
    }

    @Test
    void returnsNotFoundForMissingStation() throws Exception {
        when(stationService.getStation(999L)).thenThrow(new StationNotFoundException(999L));

        mockMvc.perform(get("/api/v1/stations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("station 999 was not found"));
    }

    @Test
    void returnsBadRequestForNonPositiveStationId() throws Exception {
        when(stationService.getStation(0L))
                .thenThrow(new IllegalArgumentException("stationId must be positive"));

        mockMvc.perform(get("/api/v1/stations/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("stationId must be positive"));
    }
}
