package com.evfinder.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class ReviewIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsListsAndDeduplicatesReview() {
        long stationId = createStation();
        CreateReviewRequest firstRequest = new CreateReviewRequest(
                "integration-user", 5, "Fast and easy to access.");
        CreateReviewRequest duplicateRequest = new CreateReviewRequest(
                "integration-user", 1, "This duplicate must not replace the first review.");

        ReviewService.CreateResult first = reviewService.createReview(stationId, firstRequest);
        ReviewService.CreateResult duplicate = reviewService.createReview(stationId, duplicateRequest);
        ReviewListResponse list = reviewService.listReviews(stationId);

        assertThat(first.created()).isTrue();
        assertThat(duplicate.created()).isFalse();
        assertThat(duplicate.review().id()).isEqualTo(first.review().id());
        assertThat(duplicate.review().rating()).isEqualTo(5);
        assertThat(list.items()).hasSize(1);
        assertThat(list.items().getFirst().userId()).isEqualTo("integration-user");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE station_id = ?", Integer.class, stationId))
                .isEqualTo(1);
    }

    private long createStation() {
        jdbcTemplate.update("""
                INSERT INTO charging_networks (network_name)
                VALUES ('Review Integration Network') AS incoming
                ON DUPLICATE KEY UPDATE network_name = incoming.network_name
                """);
        Long networkId = jdbcTemplate.queryForObject(
                "SELECT network_id FROM charging_networks WHERE network_name = 'Review Integration Network'",
                Long.class);
        jdbcTemplate.update("""
                INSERT INTO charging_stations (
                    external_id, network_id, station_name, street_address, city, state,
                    latitude, longitude
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?) AS incoming
                ON DUPLICATE KEY UPDATE station_name = incoming.station_name
                """,
                "review-integration-station", networkId, "Review Test Station", "1 Review Street",
                "Review City", "IL", 41.0, -87.0);
        return jdbcTemplate.queryForObject(
                "SELECT station_id FROM charging_stations WHERE external_id = 'review-integration-station'",
                Long.class);
    }
}
