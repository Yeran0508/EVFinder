package com.evfinder.station;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        when(stationService.searchByCity("Chicago")).thenReturn(List.of(station));

        mockMvc.perform(get("/api/v1/stations").param("city", "Chicago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Test Charging Station"))
                .andExpect(jsonPath("$.items[0].connectorTypes[0]").value("CCS"))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));

        verify(stationService).searchByCity("Chicago");
    }

    @Test
    void returnsEmptyItemsWhenNoStationsMatch() throws Exception {
        when(stationService.searchByCity("Unknown City")).thenReturn(List.of());

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
        mockMvc.perform(get("/api/v1/stations").param("city", "   "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stationService);
    }
}
