package com.evfinder.station.importer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class AfdcStationCsvReader {

    public List<AfdcStationRecord> read(Path path) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (CSVParser parser = CSVParser.parse(path, StandardCharsets.UTF_8, format)) {
            List<AfdcStationRecord> stations = new ArrayList<>();
            for (CSVRecord row : parser) {
                stations.add(toStation(row));
            }
            return stations;
        }
    }

    private AfdcStationRecord toStation(CSVRecord row) {
        return new AfdcStationRecord(
                required(row, "ID"),
                required(row, "Station Name"),
                required(row, "Street Address"),
                required(row, "City"),
                required(row, "State"),
                optional(row, "ZIP"),
                coordinate(row, "Latitude"),
                coordinate(row, "Longitude"),
                optional(row, "Status Code"),
                optional(row, "Access Code"),
                optional(row, "Access Days Time"),
                optional(row, "Facility Type"),
                integer(row, "EV Level1 EVSE Num"),
                integer(row, "EV Level2 EVSE Num"),
                integer(row, "EV DC Fast Count"),
                optional(row, "EV Pricing"),
                required(row, "EV Network"),
                optional(row, "EV Network Web"),
                ConnectorTypeNormalizer.normalize(required(row, "EV Connector Types")));
    }

    private String required(CSVRecord row, String field) {
        String value = optional(row, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required at CSV record " + row.getRecordNumber());
        }
        return value;
    }

    private String optional(CSVRecord row, String field) {
        String value = row.get(field).trim();
        return value.isEmpty() ? null : value;
    }

    private int integer(CSVRecord row, String field) {
        String value = optional(row, field);
        return value == null ? 0 : Integer.parseInt(value);
    }

    private BigDecimal decimal(CSVRecord row, String field) {
        return new BigDecimal(required(row, field));
    }

    private BigDecimal coordinate(CSVRecord row, String field) {
        return decimal(row, field).setScale(6, RoundingMode.HALF_UP);
    }
}
