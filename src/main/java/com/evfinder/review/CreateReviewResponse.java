package com.evfinder.review;

import java.time.Instant;

public record CreateReviewResponse(
        Long id,
        long stationId,
        String userId,
        int rating,
        String comment,
        Instant createdAt) {
}
