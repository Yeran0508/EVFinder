package com.evfinder.review;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class ReviewCursorCodec {

    public String encode(long reviewId) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Long.toString(reviewId).getBytes(StandardCharsets.UTF_8));
    }

    public long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return Long.MAX_VALUE;
        }
        try {
            long reviewId = Long.parseLong(new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
            if (reviewId <= 0) {
                throw new IllegalArgumentException();
            }
            return reviewId;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("invalid cursor");
        }
    }
}
