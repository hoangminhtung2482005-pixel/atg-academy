package com.example.demo.service;

import com.example.demo.security.GoogleUserAuthenticationToken;
import com.example.demo.security.GoogleUserPrincipal;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BanPickPresenceEventListener {

    private static final String ROOM_TOPIC_PREFIX = "/topic/ban-pick/";

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, SessionPresence> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomEmails = new ConcurrentHashMap<>();

    public BanPickPresenceEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String roomCode = extractRoomCode(destination);
        String email = extractEmail(accessor.getUser());
        String sessionId = accessor.getSessionId();
        if (roomCode == null || email == null || sessionId == null) {
            return;
        }

        sessions.put(sessionId, new SessionPresence(roomCode, email));
        roomEmails.computeIfAbsent(roomCode, ignored -> ConcurrentHashMap.newKeySet()).add(email);
        broadcastPresence(roomCode);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }
        SessionPresence presence = sessions.remove(sessionId);
        if (presence == null) {
            return;
        }

        boolean emailStillConnected = sessions.values().stream()
                .anyMatch(session -> presence.roomCode().equals(session.roomCode())
                        && presence.email().equals(session.email()));
        if (!emailStillConnected) {
            Set<String> emails = roomEmails.get(presence.roomCode());
            if (emails != null) {
                emails.remove(presence.email());
            }
        }
        broadcastPresence(presence.roomCode());
    }

    private void broadcastPresence(String roomCode) {
        Set<String> emails = roomEmails.getOrDefault(roomCode, Set.of());
        Map<String, Object> payload = Map.of(
                "roomCode", roomCode,
                "connectedEmails", new ArrayList<>(emails),
                "updatedAt", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/ban-pick/" + roomCode + "/presence", (Object) payload);
    }

    private String extractRoomCode(String destination) {
        if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX)) {
            return null;
        }
        String suffix = destination.substring(ROOM_TOPIC_PREFIX.length());
        if (suffix.isBlank()) {
            return null;
        }
        int slashIndex = suffix.indexOf('/');
        String roomCode = slashIndex >= 0 ? suffix.substring(0, slashIndex) : suffix;
        return roomCode.isBlank() ? null : roomCode;
    }

    private String extractEmail(Principal principal) {
        if (principal instanceof GoogleUserAuthenticationToken token
                && token.getPrincipal() instanceof GoogleUserPrincipal googleUserPrincipal) {
            return googleUserPrincipal.email();
        }
        return null;
    }

    private record SessionPresence(String roomCode, String email) {
    }
}
