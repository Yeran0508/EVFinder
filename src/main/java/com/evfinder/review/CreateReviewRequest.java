package com.evfinder.review;

public record CreateReviewRequest(
        String userId,
        Integer rating,
        String comment) {
}
