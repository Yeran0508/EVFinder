package com.evfinder.review;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evfinder.station.StationNotFoundException;
import com.evfinder.station.StationRepository;

@Service
public class ReviewService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewCursorCodec cursorCodec;

    public ReviewService(
            StationRepository stationRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository,
            ReviewCursorCodec cursorCodec) {
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.cursorCodec = cursorCodec;
    }

    @Transactional
    public CreateResult createReview(long stationId, CreateReviewRequest request) {
        validateStationId(stationId);
        validateRequest(request);
        requireStation(stationId);

        String externalUserId = request.userId().trim();
        userRepository.insertIfAbsent(externalUserId, externalUserId);
        User user = userRepository.findByExternalId(externalUserId)
                .orElseThrow(() -> new IllegalStateException("user creation failed"));

        int inserted = reviewRepository.insertIfAbsent(
                user.getUserId(), stationId, request.rating(), normalizeComment(request.comment()));
        ReviewRepository.ReviewView review = reviewRepository
                .findViewByUserIdAndStationId(user.getUserId(), stationId)
                .orElseThrow(() -> new IllegalStateException("review creation failed"));
        return new CreateResult(toCreateResponse(review, stationId), inserted == 1);
    }

    @Transactional(readOnly = true)
    public ReviewListResponse listReviews(long stationId, int limit, String cursor) {
        validateStationId(stationId);
        validateLimit(limit);
        requireStation(stationId);

        long beforeReviewId = cursorCodec.decode(cursor);
        List<ReviewRepository.ReviewView> reviews = reviewRepository.findViewsByStationIdBeforeReviewId(
                stationId, beforeReviewId, PageRequest.of(0, limit + 1));
        boolean hasMore = reviews.size() > limit;
        List<ReviewRepository.ReviewView> page = hasMore ? reviews.subList(0, limit) : reviews;
        List<ReviewResponse> items = page.stream().map(this::toResponse).toList();
        String nextCursor = hasMore ? cursorCodec.encode(page.getLast().getReviewId()) : null;
        return new ReviewListResponse(items, nextCursor);
    }

    public ReviewListResponse listReviews(long stationId) {
        return listReviews(stationId, DEFAULT_LIMIT, null);
    }

    private void validateStationId(long stationId) {
        if (stationId <= 0) {
            throw new IllegalArgumentException("stationId must be positive");
        }
    }

    private void validateRequest(CreateReviewRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.userId().trim().length() > 64) {
            throw new IllegalArgumentException("userId must contain at most 64 characters");
        }
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        if (request.comment() != null && request.comment().length() > 500) {
            throw new IllegalArgumentException("comment must contain at most 500 characters");
        }
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }

    private void requireStation(long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new StationNotFoundException(stationId);
        }
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }

    private ReviewResponse toResponse(ReviewRepository.ReviewView review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getExternalUserId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }

    private CreateReviewResponse toCreateResponse(ReviewRepository.ReviewView review, long stationId) {
        return new CreateReviewResponse(
                review.getReviewId(),
                stationId,
                review.getExternalUserId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }

    public record CreateResult(CreateReviewResponse review, boolean created) {
    }
}
