package com.example.demo.controller;

import com.example.demo.dto.user.UserContentSummaryResponse;
import com.example.demo.dto.user.UserProfileResponse;
import com.example.demo.dto.user.UserProfileUpdateRequest;
import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public UserProfileResponse getProfile(Authentication authentication) {
        return userProfileService.getCurrentProfile(currentUser(authentication));
    }

    @GetMapping("/content-summary")
    public UserContentSummaryResponse getContentSummary(Authentication authentication) {
        return userProfileService.getCurrentContentSummary(currentUser(authentication));
    }

    @PutMapping("/profile")
    public UserProfileResponse updateProfile(@RequestBody UserProfileUpdateRequest request,
                                             Authentication authentication) {
        return userProfileService.updateCurrentProfile(currentUser(authentication), request);
    }

    private GoogleUserPrincipal currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof GoogleUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        return principal;
    }
}
