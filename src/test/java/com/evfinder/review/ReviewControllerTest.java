package com.evfinder.review;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void returnsCreatedForNewReview() throws Exception {
        CreateReviewResponse response = new CreateReviewResponse(
                10L, 1L, "demo-user-1", 5, "Fast", Instant.parse("2026-11-08T12:00:00Z"));
        when(reviewService.createReview(1L, new CreateReviewRequest("demo-user-1", 5, "Fast")))
                .thenReturn(new ReviewService.CreateResult(response, true));

        mockMvc.perform(post("/api/v1/stations/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"demo-user-1","rating":5,"comment":"Fast"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stationId").value(1))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void returnsOkForDuplicateReview() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest("demo-user-1", 5, "Fast");
        CreateReviewResponse response = new CreateReviewResponse(
                10L, 1L, "demo-user-1", 5, "Fast", Instant.parse("2026-11-08T12:00:00Z"));
        when(reviewService.createReview(1L, request))
                .thenReturn(new ReviewService.CreateResult(response, false));

        mockMvc.perform(post("/api/v1/stations/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"demo-user-1","rating":5,"comment":"Fast"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void listsReviews() throws Exception {
        ReviewResponse review = new ReviewResponse(
                10L, "demo-user-1", 5, "Fast", Instant.parse("2026-11-08T12:00:00Z"));
        when(reviewService.listReviews(1L, 20, null))
                .thenReturn(new ReviewListResponse(List.of(review), null));

        mockMvc.perform(get("/api/v1/stations/1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].userId").value("demo-user-1"))
                .andExpect(jsonPath("$.items[0].rating").value(5));

        verify(reviewService).listReviews(1L, 20, null);
    }

    @Test
    void returnsBadRequestForInvalidReview() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest("demo-user-1", 6, null);
        when(reviewService.createReview(1L, request))
                .thenThrow(new IllegalArgumentException("rating must be between 1 and 5"));

        mockMvc.perform(post("/api/v1/stations/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"demo-user-1","rating":6}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("rating must be between 1 and 5"));
    }
}
