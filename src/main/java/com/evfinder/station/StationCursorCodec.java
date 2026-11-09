package com.evfinder.station;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class StationCursorCodec {

    public String encode(long stationId) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(Long.toString(stationId).getBytes(StandardCharsets.UTF_8));
    }

    public long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8);
            long stationId = Long.parseLong(decoded);
            if (stationId <= 0) {
                throw new IllegalArgumentException();
            }
            return stationId;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("invalid cursor");
        }
    }
}
