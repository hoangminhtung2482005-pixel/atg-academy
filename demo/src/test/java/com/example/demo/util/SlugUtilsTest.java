package com.example.demo.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilsTest {

    @Test
    void normalizesAzzenKaSafely() {
        assertThat(SlugUtils.toSlug("Azzen'Ka")).isEqualTo("azzen-ka");
    }

    @Test
    void normalizesVietnameseNames() {
        assertThat(SlugUtils.toSlug("Đấu Sĩ Cơ Động")).isEqualTo("dau-si-co-dong");
    }
}
