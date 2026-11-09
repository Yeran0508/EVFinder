package com.evfinder.station;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class StationPowerCursorCodecTest {

    private final StationPowerCursorCodec codec = new StationPowerCursorCodec();

    @Test
    void encodesPowerOrderAndStationId() {
        String cursor = codec.encode("desc", new BigDecimal("350.00"), 123L);

        StationPowerCursorCodec.PowerCursor decoded = codec.decode(cursor, "desc");

        assertThat(decoded.powerSortValue()).isEqualByComparingTo("350.00");
        assertThat(decoded.stationId()).isEqualTo(123L);
    }

    @Test
    void rejectsCursorFromDifferentOrder() {
        String cursor = codec.encode("desc", new BigDecimal("350.00"), 123L);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> codec.decode(cursor, "asc"))
                .withMessage("invalid cursor");
    }
}
