package com.example.demo.config;

import com.example.demo.security.GoogleJwtAuthenticator;
import com.example.demo.security.GoogleUserAuthenticationToken;
import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.BanPickRoomService;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final GoogleJwtAuthenticator googleJwtAuthenticator;
    private final BanPickRoomService banPickRoomService;
    private final CorsProperties corsProperties;

    public WebSocketConfig(GoogleJwtAuthenticator googleJwtAuthenticator,
                           BanPickRoomService banPickRoomService,
                           CorsProperties corsProperties) {
        this.googleJwtAuthenticator = googleJwtAuthenticator;
        this.banPickRoomService = banPickRoomService;
        this.corsProperties = corsProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = corsProperties.getAllowedOrigins().toArray(String[]::new);
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
                    if (!StringUtils.hasText(authorization)) {
                        throw new BadCredentialsException("Missing WebSocket Authorization header");
                    }
                    GoogleUserAuthenticationToken authentication =
                            googleJwtAuthenticator.authenticateBearerToken(authorization);
                    accessor.setUser(authentication);
                }
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    validateBanPickTopicSubscription(accessor);
                }
                return message;
            }
        });
    }

    private void validateBanPickTopicSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        String prefix = "/topic/ban-pick/";
        if (!StringUtils.hasText(destination) || !destination.startsWith(prefix)) {
            return;
        }
        if (!(accessor.getUser() instanceof GoogleUserAuthenticationToken token)
                || !(token.getPrincipal() instanceof GoogleUserPrincipal principal)) {
            throw new BadCredentialsException("Missing WebSocket user");
        }
        String suffix = destination.substring(prefix.length());
        int slashIndex = suffix.indexOf('/');
        String encodedRoomCode = slashIndex >= 0 ? suffix.substring(0, slashIndex) : suffix;
        String roomCode = URLDecoder.decode(encodedRoomCode, StandardCharsets.UTF_8);
        banPickRoomService.getRoomState(roomCode, principal);
    }
}
