package com.evfinder.station;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import com.evfinder.station.importer.AfdcStationImportService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class StationImportIntegrationTest {

    private static final List<String> EXPECTED_EXTERNAL_IDS = List.of(
            "40794", "149766", "98760", "46798", "49660",
            "42460", "60165", "33921", "42462", "46817",
            "47199", "95170", "102152", "103918", "114141");

    @Autowired
    private AfdcStationImportService importService;

    @Autowired
    private StationRepository stationRepository;

    @Test
    void importsAndQueriesAllFifteenFixtureStations() throws Exception {
        Path fixture = new ClassPathResource("fixtures/afdc-stations-sample.csv")
                .getFile()
                .toPath();

        AfdcStationImportService.ImportResult result = importService.importFile(fixture);
        List<Station> importedStations = stationRepository.findAllByExternalIdIn(EXPECTED_EXTERNAL_IDS);

        assertThat(result.totalRows()).isEqualTo(15);
        assertThat(result.importedStations()).isEqualTo(15);
        assertThat(importedStations)
                .extracting(Station::getExternalId)
                .containsExactlyInAnyOrderElementsOf(EXPECTED_EXTERNAL_IDS);
    }
}
