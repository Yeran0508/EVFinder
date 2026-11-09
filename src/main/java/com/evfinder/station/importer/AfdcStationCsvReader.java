package com.evfinder.station.importer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class AfdcStationCsvReader {

    public List<AfdcStationRecord> read(Path path) throws IOException {
        return readWithMetadata(path).stations();
    }

    public ReadResult readWithMetadata(Path path) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (CSVParser parser = CSVParser.parse(path, StandardCharsets.UTF_8, format)) {
            Map<String, StationAccumulator> stations = new LinkedHashMap<>();
            int sourceRows = 0;
            for (CSVRecord row : parser) {
                sourceRows++;
                AfdcStationRecord station = toStation(row);
                stations.computeIfAbsent(
                                station.externalId(),
                                ignored -> new StationAccumulator(station))
                        .add(row);
            }
            return new ReadResult(
                    stations.values().stream().map(StationAccumulator::build).toList(),
                    sourceRows);
        }
    }

    private AfdcStationRecord toStation(CSVRecord row) {
        return new AfdcStationRecord(
                required(row, "ID"),
                required(row, "Station Name"),
                optional(row, "Street Address"),
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

    private Integer optionalInteger(CSVRecord row, String field) {
        if (!row.isMapped(field)) {
            return null;
        }
        String value = optional(row, field);
        return value == null ? null : Integer.valueOf(value);
    }

    private BigDecimal optionalDecimal(CSVRecord row, String field) {
        if (!row.isMapped(field)) {
            return null;
        }
        String value = optional(row, field);
        return value == null ? null : new BigDecimal(value);
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

    public record ReadResult(List<AfdcStationRecord> stations, int sourceRows) {
        public ReadResult {
            stations = List.copyOf(stations);
        }
    }

    private final class StationAccumulator {

        private static final List<ConnectorColumns> DETAILED_CONNECTORS = List.of(
                new ConnectorColumns("J1772", "J1772", "EV J1772 Connector Count", "EV J1772 Power Output (kW)"),
                new ConnectorColumns("CCS", "J1772COMBO", "EV CCS Connector Count", "EV CCS Power Output (kW)"),
                new ConnectorColumns("CHAdeMO", "CHADEMO", "EV CHAdeMO Connector Count", "EV CHAdeMO Power Output (kW)"),
                new ConnectorColumns("NACS", "TESLA", "EV J3400 Connector Count", "EV J3400 Power Output (kW)"),
                new ConnectorColumns("MCS", "J3271", "EV J3271 Connector Count", "EV J3271 Power Output (kW)"));

        private final AfdcStationRecord station;
        private final Map<String, ConnectorAggregate> connectors = new LinkedHashMap<>();

        private StationAccumulator(AfdcStationRecord station) {
            this.station = station;
            station.connectors().forEach(connector -> connectors.put(
                    connector.name(),
                    new ConnectorAggregate(connector.name(), connector.afdcCode())));
        }

        private void add(CSVRecord row) {
            for (ConnectorColumns columns : DETAILED_CONNECTORS) {
                Integer count = optionalInteger(row, columns.countColumn());
                BigDecimal power = optionalDecimal(row, columns.powerColumn());
                if ((count == null || count == 0) && power == null) {
                    continue;
                }
                connectors.computeIfAbsent(
                                columns.name(),
                                ignored -> new ConnectorAggregate(columns.name(), columns.afdcCode()))
                        .add(count, power);
            }
        }

        private AfdcStationRecord build() {
            List<ConnectorTypeNormalizer.NormalizedConnector> aggregatedConnectors = connectors.values().stream()
                    .map(ConnectorAggregate::build)
                    .toList();
            return new AfdcStationRecord(
                    station.externalId(), station.stationName(), station.streetAddress(), station.city(),
                    station.state(), station.zipCode(), station.latitude(), station.longitude(),
                    station.statusCode(), station.accessCode(), station.accessDaysTime(), station.facilityType(),
                    station.level1EvseCount(), station.level2EvseCount(), station.dcFastCount(),
                    station.pricingText(), station.networkName(), station.networkUrl(), aggregatedConnectors);
        }
    }

    private record ConnectorColumns(
            String name,
            String afdcCode,
            String countColumn,
            String powerColumn) {
    }

    private static final class ConnectorAggregate {

        private final String name;
        private final String afdcCode;
        private Integer count;
        private BigDecimal maxPowerKw;

        private ConnectorAggregate(String name, String afdcCode) {
            this.name = name;
            this.afdcCode = afdcCode;
        }

        private void add(Integer additionalCount, BigDecimal powerKw) {
            if (additionalCount != null) {
                count = (count == null ? 0 : count) + additionalCount;
            }
            if (powerKw != null && (maxPowerKw == null || powerKw.compareTo(maxPowerKw) > 0)) {
                maxPowerKw = powerKw;
            }
        }

        private ConnectorTypeNormalizer.NormalizedConnector build() {
            return new ConnectorTypeNormalizer.NormalizedConnector(name, afdcCode, count, maxPowerKw);
        }
    }
}
