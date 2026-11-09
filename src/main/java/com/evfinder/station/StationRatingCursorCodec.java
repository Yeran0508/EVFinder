package com.evfinder.station;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class StationRatingCursorCodec {

    public String encode(String order, BigDecimal ratingSortValue, long stationId) {
        String value = "%s|%s|%d".formatted(order, ratingSortValue.toPlainString(), stationId);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public RatingCursor decode(String cursor, String order) {
        if (cursor == null || cursor.isBlank()) {
            BigDecimal initialRating = order.equals("desc")
                    ? new BigDecimal("6")
                    : new BigDecimal("-1");
            return new RatingCursor(initialRating, 0);
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 3 || !parts[0].equals(order)) {
                throw new IllegalArgumentException();
            }
            BigDecimal rating = new BigDecimal(parts[1]);
            long stationId = Long.parseLong(parts[2]);
            if (stationId <= 0 || rating.compareTo(new BigDecimal("-1")) < 0
                    || rating.compareTo(new BigDecimal("6")) > 0) {
                throw new IllegalArgumentException();
            }
            return new RatingCursor(rating, stationId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("invalid cursor");
        }
    }

    public record RatingCursor(BigDecimal ratingSortValue, long stationId) {
    }
}
