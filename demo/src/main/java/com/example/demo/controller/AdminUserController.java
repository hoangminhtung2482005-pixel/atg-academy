package com.example.demo.controller;

import com.example.demo.dto.admin.AdminUserPageResponse;
import com.example.demo.dto.admin.AdminUserResponse;
import com.example.demo.dto.admin.AdminUserUpdateRequest;
import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public AdminUserPageResponse listUsers(@RequestParam(required = false) String search,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Page<AdminUserResponse> users = adminUserService.findUsers(search, role, status, page, size);
        return AdminUserPageResponse.from(users);
    }

    @GetMapping("/{id}")
    public AdminUserResponse getUser(@PathVariable Long id) {
        return adminUserService.getUser(id);
    }

    @PutMapping("/{id}")
    public AdminUserResponse updateUser(@PathVariable Long id,
                                        @RequestBody AdminUserUpdateRequest request,
                                        Authentication authentication) {
        return adminUserService.updateUser(id, request, currentAdminEmail(authentication));
    }

    private String currentAdminEmail(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof GoogleUserPrincipal principal) {
            return principal.email();
        }
        return null;
    }
}
