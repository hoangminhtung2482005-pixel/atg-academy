package com.example.demo.security;

import com.example.demo.config.CorsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({AppSecurityProperties.class, CorsProperties.class})
public class SecurityConfig {

    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   GoogleJwtAuthenticationFilter googleJwtAuthenticationFilter,
                                                   ApiAuthenticationEntryPoint authenticationEntryPoint,
                                                   ApiAccessDeniedHandler accessDeniedHandler,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                // CORS: Cấu hình global từ corsConfigurationSource bean
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // CSRF disabled vì API stateless dùng JWT Bearer token
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                        .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/wiki/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/spells/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/enchantments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/esports/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/guides/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tier-lists/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ban-pick/history/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ban-pick/leaderboard").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/ban-pick/**").authenticated()
                        .requestMatchers("/api/guides/**").authenticated()
                        .requestMatchers("/api/tier-lists/**").authenticated()
                        .requestMatchers("/api/users/me/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(googleJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/ws/**", config);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder(AppSecurityProperties securityProperties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                issuerValidator(),
                audienceValidator(securityProperties),
                emailValidator()
        ));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> {
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
            boolean valid = "https://accounts.google.com".equals(issuer)
                    || "accounts.google.com".equals(issuer);
            if (valid) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "Google token issuer is invalid.",
                    null
            ));
        };
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(AppSecurityProperties securityProperties) {
        return jwt -> {
            String clientId = securityProperties.getGoogleClientId();
            if (!StringUtils.hasText(clientId) || jwt.getAudience().contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "Google token audience does not match the configured client id.",
                    null
            ));
        };
    }

    private OAuth2TokenValidator<Jwt> emailValidator() {
        return jwt -> {
            String email = jwt.getClaimAsString("email");
            Object emailVerified = jwt.getClaims().get("email_verified");
            boolean verified = Boolean.TRUE.equals(emailVerified)
                    || "true".equalsIgnoreCase(String.valueOf(emailVerified));

            if (StringUtils.hasText(email) && verified) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "Google token does not contain a verified email.",
                    null
            ));
        };
    }
}
