package com.evfinder.station;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class StationRatingIntegrationTest {

    @Autowired
    private StationService stationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void aggregatesRatingsAndPaginatesInDescendingOrder() {
        long fiveStarStation = createStation("rating-sort-5", "Five Star Station");
        long threeStarStation = createStation("rating-sort-3", "Three Star Station");
        long unratedStation = createStation("rating-sort-none", "Unrated Station");
        createReview("rating-user-5", fiveStarStation, 5);
        createReview("rating-user-3b", fiveStarStation, 3);
        createReview("rating-user-3", threeStarStation, 3);

        StationSearchResponse first = stationService.search(
                "Rating Sort City", null, "rating", "desc", 1, null);
        StationSearchResponse second = stationService.search(
                "Rating Sort City", null, "rating", "desc", 1, first.nextCursor());
        StationSearchResponse third = stationService.search(
                "Rating Sort City", null, "rating", "desc", 1, second.nextCursor());
        StationDetailResponse detail = stationService.getStation(fiveStarStation);
        StationSearchResponse ascending = stationService.search(
                "Rating Sort City", null, "rating", "asc", 3, null);

        assertThat(first.items().getFirst().id()).isEqualTo(fiveStarStation);
        assertThat(first.items().getFirst().averageRating()).isEqualByComparingTo("4.0");
        assertThat(first.items().getFirst().reviewCount()).isEqualTo(2);
        assertThat(second.items().getFirst().id()).isEqualTo(threeStarStation);
        assertThat(third.items().getFirst().id()).isEqualTo(unratedStation);
        assertThat(third.items().getFirst().averageRating()).isNull();
        assertThat(third.items().getFirst().reviewCount()).isZero();
        assertThat(detail.averageRating()).isEqualByComparingTo("4.0");
        assertThat(detail.reviewCount()).isEqualTo(2);
        assertThat(ascending.items())
                .extracting(StationResponse::id)
                .containsExactly(threeStarStation, fiveStarStation, unratedStation);
    }

    private long createStation(String externalId, String name) {
        jdbcTemplate.update("""
                INSERT INTO charging_networks (network_name)
                VALUES ('Rating Integration Network') AS incoming
                ON DUPLICATE KEY UPDATE network_name = incoming.network_name
                """);
        Long networkId = jdbcTemplate.queryForObject(
                "SELECT network_id FROM charging_networks WHERE network_name = 'Rating Integration Network'",
                Long.class);
        jdbcTemplate.update("""
                INSERT INTO charging_stations (
                    external_id, network_id, station_name, street_address, city, state,
                    latitude, longitude
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?) AS incoming
                ON DUPLICATE KEY UPDATE station_name = incoming.station_name
                """,
                externalId, networkId, name, "1 Rating Street", "Rating Sort City", "IL", 41.0, -87.0);
        return jdbcTemplate.queryForObject(
                "SELECT station_id FROM charging_stations WHERE external_id = ?",
                Long.class,
                externalId);
    }

    private void createReview(String externalUserId, long stationId, int rating) {
        jdbcTemplate.update("""
                INSERT INTO users (external_id, display_name)
                VALUES (?, ?) AS incoming
                ON DUPLICATE KEY UPDATE display_name = incoming.display_name
                """, externalUserId, externalUserId);
        Long userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE external_id = ?",
                Long.class,
                externalUserId);
        jdbcTemplate.update(
                "INSERT INTO reviews (user_id, station_id, rating) VALUES (?, ?, ?)",
                userId, stationId, rating);
    }
}
