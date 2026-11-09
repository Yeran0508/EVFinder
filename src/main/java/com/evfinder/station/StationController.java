package com.evfinder.station;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public StationSearchResponse searchByCity(
            @RequestParam String city,
            @RequestParam(required = false) String connectorType,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        return stationService.search(city, connectorType, limit, cursor);
    }

    @GetMapping("/{stationId}")
    public StationDetailResponse getStation(@PathVariable long stationId) {
        return stationService.getStation(stationId);
    }
}
