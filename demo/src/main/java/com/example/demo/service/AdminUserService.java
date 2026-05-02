package com.example.demo.service;

import com.example.demo.dto.admin.AdminUserResponse;
import com.example.demo.dto.admin.AdminUserUpdateRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> findUsers(String search,
                                             String role,
                                             String status,
                                             int page,
                                             int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"))
        );

        Specification<User> spec = buildSpecification(search, role, status);
        return userRepository.findAll(spec, pageable).map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long id) {
        return AdminUserResponse.from(findUser(id));
    }

    @Transactional
    public AdminUserResponse updateUser(Long id,
                                        AdminUserUpdateRequest request,
                                        String currentAdminEmail) {
        User user = findUser(id);
        boolean editingSelf = sameEmail(user.getEmail(), currentAdminEmail);

        if (request.name() != null) {
            String name = request.name().trim();
            if (!StringUtils.hasText(name)) {
                throw badRequest("Ten nguoi dung khong duoc de trong");
            }
            if (name.length() > 80) {
                throw badRequest("Ten hien thi toi da 80 ky tu");
            }
            user.setDisplayName(name);
        }

        if (request.avatarUrl() != null) {
            user.setAvatarUrl(blankToNull(request.avatarUrl()));
        }

        if (request.role() != null) {
            UserRole nextRole = parseRole(request.role());
            if (editingSelf && nextRole != UserRole.ADMIN) {
                throw badRequest("Admin khong the tu go vai tro ADMIN cua chinh minh");
            }
            user.setRole(nextRole.getStorageValue());
        }

        if (request.status() != null) {
            UserStatus nextStatus = parseStatus(request.status());
            if (editingSelf && nextStatus == UserStatus.LOCKED) {
                throw badRequest("Admin khong the tu khoa tai khoan cua chinh minh");
            }
            user.setStatus(nextStatus);
        }

        if (request.note() != null) {
            user.setNote(blankToNull(request.note()));
        }

        return AdminUserResponse.from(userRepository.save(user));
    }

    private Specification<User> buildSpecification(String search, String role, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String keyword = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), keyword),
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(cb.coalesce(root.get("displayName"), "")), keyword)
                ));
            }

            if (StringUtils.hasText(role)) {
                String normalizedRole = parseRole(role).getStorageValue().toLowerCase(Locale.ROOT);
                predicates.add(cb.equal(cb.lower(root.get("role")), normalizedRole));
            }

            if (StringUtils.hasText(status)) {
                UserStatus normalizedStatus = parseStatus(status);
                if (normalizedStatus == UserStatus.ACTIVE) {
                    predicates.add(cb.or(
                            cb.equal(root.get("status"), UserStatus.ACTIVE),
                            cb.isNull(root.get("status"))
                    ));
                } else {
                    predicates.add(cb.equal(root.get("status"), normalizedStatus));
                }
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay nguoi dung"));
    }

    private UserRole parseRole(String value) {
        try {
            return UserRole.from(value);
        } catch (IllegalArgumentException ex) {
            throw badRequest(ex.getMessage());
        }
    }

    private UserStatus parseStatus(String value) {
        try {
            return UserStatus.from(value);
        } catch (IllegalArgumentException ex) {
            throw badRequest(ex.getMessage());
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private boolean sameEmail(String left, String right) {
        return StringUtils.hasText(left)
                && StringUtils.hasText(right)
                && Objects.equals(
                left.trim().toLowerCase(Locale.ROOT),
                right.trim().toLowerCase(Locale.ROOT)
        );
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
