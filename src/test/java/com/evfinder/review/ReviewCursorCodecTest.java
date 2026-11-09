package com.evfinder.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class ReviewCursorCodecTest {

    private final ReviewCursorCodec codec = new ReviewCursorCodec();

    @Test
    void encodesAndDecodesReviewId() {
        assertThat(codec.decode(codec.encode(25L))).isEqualTo(25L);
    }

    @Test
    void usesMaximumIdWhenCursorIsMissing() {
        assertThat(codec.decode(null)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void rejectsInvalidCursor() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> codec.decode("invalid"))
                .withMessage("invalid cursor");
    }
}
