package com.evfinder.review;

import java.util.List;

public record ReviewListResponse(
        List<ReviewResponse> items,
        String nextCursor) {

    public ReviewListResponse {
        items = List.copyOf(items);
    }
}
