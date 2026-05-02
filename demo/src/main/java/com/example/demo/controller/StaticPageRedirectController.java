package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StaticPageRedirectController {

    @GetMapping({"/", "/index.html"})
    public String redirectHome() {
        return "redirect:/html/index.html";
    }

    @GetMapping({
            "/account.html",
            "/admin.html",
            "/ban-pick.html",
            "/ban-pick-leaderboard.html",
            "/ban-pick-profile.html",
            "/ban-pick-result.html",
            "/content.html",
            "/create-guide.html",
            "/esports.html",
            "/giao-an.html",
            "/guide-detail.html",
            "/header.html",
            "/tactics-guides.html",
            "/tier-list-detail.html",
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

    @GetMapping("/guides")
    public String redirectGuidesPage() {
        return "redirect:/html/giao-an.html";
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
    public String forwardBanPickResult(@PathVariable Long id) {
        return "forward:/html/ban-pick-result.html";
    }

    @GetMapping("/ban-pick/profile")
    public String redirectBanPickProfile() {
        return "redirect:/html/ban-pick-profile.html";
    }

    @GetMapping("/ban-pick/leaderboard")
    public String redirectBanPickLeaderboard() {
        return "redirect:/html/ban-pick-leaderboard.html";
    }
}
