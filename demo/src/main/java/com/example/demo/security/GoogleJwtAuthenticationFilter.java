package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GoogleJwtAuthenticationFilter extends OncePerRequestFilter {

    private final GoogleJwtAuthenticator googleJwtAuthenticator;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public GoogleJwtAuthenticationFilter(GoogleJwtAuthenticator googleJwtAuthenticator,
                                         AuthenticationEntryPoint authenticationEntryPoint) {
        this.googleJwtAuthenticator = googleJwtAuthenticator;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            GoogleUserAuthenticationToken authentication = googleJwtAuthenticator.authenticateBearerToken(authHeader);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException | BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    ex instanceof BadCredentialsException badCredentialsException
                            ? badCredentialsException
                            : new BadCredentialsException("Google JWT is invalid", ex)
            );
        }
    }
}
