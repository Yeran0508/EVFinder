package com.evfinder.station;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class StationCursorCodecTest {

    private final StationCursorCodec cursorCodec = new StationCursorCodec();

    @Test
    void encodesAndDecodesStationId() {
        String cursor = cursorCodec.encode(123L);

        assertThat(cursorCodec.decode(cursor)).isEqualTo(123L);
    }

    @Test
    void treatsMissingCursorAsBeginning() {
        assertThat(cursorCodec.decode(null)).isZero();
        assertThat(cursorCodec.decode(" ")).isZero();
    }

    @Test
    void rejectsMalformedCursor() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> cursorCodec.decode("not-a-cursor"))
                .withMessage("invalid cursor");
    }
}
