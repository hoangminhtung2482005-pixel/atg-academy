package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickRoomStateResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class BanPickRoomBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public BanPickRoomBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(BanPickRoomStateResponse roomState) {
        if (roomState == null || roomState.roomCode() == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/ban-pick/" + roomState.roomCode(), roomState.withoutCurrentUserSide());
    }
}
