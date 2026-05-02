package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Component
public class GoogleJwtAuthenticator {

    private final JwtDecoder jwtDecoder;
    private final AppSecurityProperties securityProperties;
    private final UserRepository userRepository;

    public GoogleJwtAuthenticator(JwtDecoder jwtDecoder,
                                  AppSecurityProperties securityProperties,
                                  UserRepository userRepository) {
        this.jwtDecoder = jwtDecoder;
        this.securityProperties = securityProperties;
        this.userRepository = userRepository;
    }

    public GoogleUserAuthenticationToken authenticateBearerToken(String bearerToken) {
        if (!StringUtils.hasText(bearerToken)) {
            throw new BadCredentialsException("Missing Google JWT");
        }

        String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;
        Jwt jwt = jwtDecoder.decode(token);
        String email = jwt.getClaimAsString("email");
        String configuredRole = securityProperties.resolveRole(email);
        User user = findOrCreateUser(jwt, email, configuredRole);
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BadCredentialsException("Tai khoan da bi khoa");
        }

        String role = resolveAuthenticationRole(user, configuredRole);
        GoogleUserPrincipal principal = new GoogleUserPrincipal(
                email,
                user.getName(),
                user.getAvatarUrl(),
                role
        );
        return new GoogleUserAuthenticationToken(
                principal,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    private User findOrCreateUser(Jwt jwt, String email, String configuredRole) {
        return userRepository.findByEmail(email)
                .map(existing -> updateMissingProfile(existing, jwt, configuredRole))
                .orElseGet(() -> createUser(jwt, email, configuredRole));
    }

    private User createUser(Jwt jwt, String email, String configuredRole) {
        User user = new User();
        user.setEmail(email);
        user.setName(defaultClaim(jwt, "name", "User"));
        user.setAvatarUrl(defaultClaim(jwt, "picture", ""));
        user.setRole(UserRole.from(configuredRole).getStorageValue());
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private User updateMissingProfile(User user, Jwt jwt, String configuredRole) {
        boolean changed = false;
        if (!StringUtils.hasText(user.getName())) {
            user.setName(defaultClaim(jwt, "name", "User"));
            changed = true;
        }
        if (!StringUtils.hasText(user.getAvatarUrl())) {
            user.setAvatarUrl(defaultClaim(jwt, "picture", ""));
            changed = true;
        }
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole(UserRole.from(configuredRole).getStorageValue());
            changed = true;
        }
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
            changed = true;
        }
        return changed ? userRepository.save(user) : user;
    }

    private String resolveAuthenticationRole(User user, String configuredRole) {
        if ("ADMIN".equalsIgnoreCase(configuredRole)) {
            return UserRole.ADMIN.name();
        }
        return UserRole.from(user.getRole()).name().toUpperCase(Locale.ROOT);
    }

    private String defaultClaim(Jwt jwt, String claimName, String fallback) {
        String value = jwt.getClaimAsString(claimName);
        return StringUtils.hasText(value) ? value : fallback;
    }
}
