package com.evfinder.review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        String userId,
        int rating,
        String comment,
        Instant createdAt) {
}
