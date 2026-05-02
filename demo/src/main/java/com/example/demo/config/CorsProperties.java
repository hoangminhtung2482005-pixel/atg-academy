package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Bind giá trị app.cors.* từ application.properties.
 * Dùng chung cho cả SecurityConfig (HTTP CORS) và WebSocketConfig (STOMP origins).
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Danh sách origin được phép truy cập API và WebSocket.
     * Ví dụ: https://frigidly-attribute-step.ngrok-free.dev,http://localhost:8080
     */
    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins != null ? allowedOrigins : new ArrayList<>();
    }
}
