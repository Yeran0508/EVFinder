package com.evfinder.station;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class StationPowerCursorCodec {

    public String encode(String order, BigDecimal powerSortValue, long stationId) {
        String value = "%s|%s|%d".formatted(order, powerSortValue.toPlainString(), stationId);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public PowerCursor decode(String cursor, String order) {
        if (cursor == null || cursor.isBlank()) {
            return new PowerCursor(BigDecimal.ZERO, 0);
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 3 || !parts[0].equals(order)) {
                throw new IllegalArgumentException();
            }
            BigDecimal power = new BigDecimal(parts[1]);
            long stationId = Long.parseLong(parts[2]);
            if (stationId <= 0 || power.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException();
            }
            return new PowerCursor(power, stationId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("invalid cursor");
        }
    }

    public record PowerCursor(BigDecimal powerSortValue, long stationId) {
    }
}
