package com.evfinder.station.importer;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "evfinder.import.file")
public class AfdcStationImportRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AfdcStationImportRunner.class);

    private final AfdcStationImportService importService;
    private final Path importFile;

    public AfdcStationImportRunner(
            AfdcStationImportService importService,
            @Value("${evfinder.import.file}") String importFile) {
        this.importService = importService;
        this.importFile = Path.of(importFile);
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        AfdcStationImportService.ImportResult result = importService.importFile(importFile);
        logger.info("AFDC import completed: totalRows={}, importedStations={}",
                result.totalRows(), result.importedStations());
    }
}
