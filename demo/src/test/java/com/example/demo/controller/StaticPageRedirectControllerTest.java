package com.example.demo.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StaticPageRedirectControllerTest {

    private final StaticPageRedirectController controller = new StaticPageRedirectController();

    @Test
    void redirectBanPickEntryUsesFreeModeByDefault() {
        assertThat(controller.redirectBanPickEntry(null, null))
                .isEqualTo("redirect:/html/ban-pick-free.html");
    }

    @Test
    void redirectBanPickEntryKeepsSoloRoomLinks() {
        assertThat(controller.redirectBanPickEntry("ROOM-123", null))
                .isEqualTo("redirect:/html/ban-pick-solo.html?room=ROOM-123");
    }

    @Test
    void redirectBanPickEntrySupportsExplicitMode() {
        assertThat(controller.redirectBanPickEntry(null, "standard"))
                .isEqualTo("redirect:/html/ban-pick-standard.html");
        assertThat(controller.redirectBanPickEntry(null, "solo"))
                .isEqualTo("redirect:/html/ban-pick-solo.html");
    }

    @Test
    void redirectTierListCommunityPagesUseSupportedTargets() {
        assertThat(controller.redirectTierListRecommendedPage())
                .isEqualTo("redirect:/html/tier-list.html");
        assertThat(controller.redirectDeprecatedTierListRecommendedHtml())
                .isEqualTo("redirect:/html/tier-list.html");
        assertThat(controller.redirectTierListAllPage())
                .isEqualTo("redirect:/html/tier-list-all.html");
        assertThat(controller.redirectTierListMinePage())
                .isEqualTo("redirect:/html/tier-list-mine.html");
    }

    @Test
    void redirectEsportsDataPageUsesSupportedTarget() {
        assertThat(controller.redirectEsportsDataPage())
                .isEqualTo("redirect:/html/esports-data.html");
    }
}
