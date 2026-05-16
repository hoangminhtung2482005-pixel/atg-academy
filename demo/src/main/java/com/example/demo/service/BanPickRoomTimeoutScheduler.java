package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickRoomStateResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(prefix = "spring.task.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BanPickRoomTimeoutScheduler {

    private final BanPickRoomService banPickRoomService;
    private final BanPickRoomBroadcaster banPickRoomBroadcaster;

    public BanPickRoomTimeoutScheduler(BanPickRoomService banPickRoomService,
                                       BanPickRoomBroadcaster banPickRoomBroadcaster) {
        this.banPickRoomService = banPickRoomService;
        this.banPickRoomBroadcaster = banPickRoomBroadcaster;
    }

    @Scheduled(fixedDelay = 1000)
    public void resolveExpiredPhases() {
        LocalDateTime now = LocalDateTime.now();
        for (String roomCode : banPickRoomService.findExpiredRoomCodes(now)) {
            banPickRoomService.resolveExpiredPhase(roomCode)
                    .ifPresent(this::broadcast);
        }
        for (BanPickRoomStateResponse roomState : banPickRoomService.resolveExpiredDisconnectGraceWindows(now)) {
            broadcast(roomState);
        }
    }

    private void broadcast(BanPickRoomStateResponse roomState) {
        banPickRoomBroadcaster.broadcast(roomState);
    }
}
