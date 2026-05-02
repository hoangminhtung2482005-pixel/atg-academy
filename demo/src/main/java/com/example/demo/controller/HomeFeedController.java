package com.example.demo.controller;

import com.example.demo.dto.home.HomeFeedItemResponse;
import com.example.demo.service.HomeFeedService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
public class HomeFeedController {

    private final HomeFeedService homeFeedService;

    public HomeFeedController(HomeFeedService homeFeedService) {
        this.homeFeedService = homeFeedService;
    }

    @GetMapping("/feed")
    public List<HomeFeedItemResponse> getFeed() {
        return homeFeedService.getHomeFeed();
    }
}
