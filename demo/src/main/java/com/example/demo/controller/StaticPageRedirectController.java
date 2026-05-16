package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class StaticPageRedirectController {

    @GetMapping({"/", "/index.html"})
    public String redirectHome() {
        return "redirect:/html/index.html";
    }

    @GetMapping({
            "/account.html",
            "/admin.html",
            "/ban-pick-leaderboard.html",
            "/create-guide.html",
            "/esports.html",
            "/esports-data.html",
            "/giao-an.html",
            "/guide-detail.html",
            "/header.html",
            "/tier-list-all.html",
            "/tier-list-detail.html",
            "/tier-list-mine.html",
            "/tier-list.html",
            "/wiki.html"
    })
    public String redirectLegacyPage(HttpServletRequest request) {
        return "redirect:/html" + request.getServletPath();
    }

    @GetMapping({"/meta.html", "/html/meta.html"})
    public String redirectMetaPage() {
        return "redirect:/html/tier-list.html";
    }

    @GetMapping("/tier-list")
    public String redirectTierListPage() {
        return "redirect:/html/tier-list.html";
    }

    @GetMapping("/tier-list/recommended")
    public String redirectTierListRecommendedPage() {
        return "redirect:/html/tier-list.html";
    }

    @GetMapping({"/tier-list-recommended.html", "/html/tier-list-recommended.html"})
    public String redirectDeprecatedTierListRecommendedHtml() {
        return "redirect:/html/tier-list.html";
    }

    @GetMapping("/tier-list/all")
    public String redirectTierListAllPage() {
        return "redirect:/html/tier-list-all.html";
    }

    @GetMapping("/tier-list/mine")
    public String redirectTierListMinePage() {
        return "redirect:/html/tier-list-mine.html";
    }

    @GetMapping("/esports/data")
    public String redirectEsportsDataPage() {
        return "redirect:/html/esports-data.html";
    }

    @GetMapping("/guides")
    public String redirectGuidesPage() {
        return "redirect:/html/giao-an.html";
    }

    @GetMapping({"/tactics-guides.html", "/html/tactics-guides.html"})
    public String redirectLegacyTacticsGuidesPage() {
        return "redirect:/html/giao-an.html";
    }

    @GetMapping({"/ban-pick", "/ban-pick.html", "/html/ban-pick.html"})
    public String redirectBanPickEntry(
            @RequestParam(required = false) String room,
            @RequestParam(required = false) String mode
    ) {
        if (StringUtils.hasText(room)) {
            return "redirect:" + UriComponentsBuilder
                    .fromPath("/html/ban-pick-solo.html")
                    .queryParam("room", room.trim())
                    .build()
                    .encode()
                    .toUriString();
        }

        String normalizedMode = StringUtils.hasText(mode) ? mode.trim().toLowerCase() : "";
        return switch (normalizedMode) {
            case "standard" -> "redirect:/html/ban-pick-standard.html";
            case "solo", "solo-1v1" -> "redirect:/html/ban-pick-solo.html";
            case "free", "" -> "redirect:/html/ban-pick-free.html";
            default -> "redirect:/html/ban-pick-free.html";
        };
    }

    @GetMapping("/tier-list/{id}")
    public String redirectTierListDetail(@PathVariable Long id) {
        return "redirect:/html/tier-list-detail.html?id=" + id;
    }

    @GetMapping("/guides/{id}")
    public String redirectGuideDetail(@PathVariable Long id) {
        return "redirect:/html/guide-detail.html?id=" + id;
    }

    @GetMapping("/ban-pick/result/{id}")
    public String redirectLegacyBanPickResult(@PathVariable Long id) {
        return "redirect:/html/ban-pick-solo.html";
    }

    @GetMapping({"/ban-pick-result.html", "/html/ban-pick-result.html"})
    public String redirectLegacyBanPickResultPage() {
        return "redirect:/html/ban-pick-solo.html";
    }

    @GetMapping({"/ban-pick/profile", "/ban-pick-profile.html", "/html/ban-pick-profile.html"})
    public String redirectBanPickProfile() {
        return "redirect:/html/ban-pick-solo.html";
    }

    @GetMapping("/ban-pick/leaderboard")
    public String redirectBanPickLeaderboard() {
        return "redirect:/html/ban-pick-leaderboard.html";
    }
}
