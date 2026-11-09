package com.evfinder.station;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class StationRatingCursorCodecTest {

    private final StationRatingCursorCodec codec = new StationRatingCursorCodec();

    @Test
    void encodesRatingOrderAndStationId() {
        String cursor = codec.encode("desc", new BigDecimal("4.50"), 123L);

        StationRatingCursorCodec.RatingCursor decoded = codec.decode(cursor, "desc");

        assertThat(decoded.ratingSortValue()).isEqualByComparingTo("4.50");
        assertThat(decoded.stationId()).isEqualTo(123L);
    }

    @Test
    void rejectsCursorFromDifferentOrder() {
        String cursor = codec.encode("desc", new BigDecimal("4.50"), 123L);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> codec.decode(cursor, "asc"))
                .withMessage("invalid cursor");
    }
}
