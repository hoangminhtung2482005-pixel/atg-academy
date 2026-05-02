package com.example.demo.controller;

import com.example.demo.dto.banpick.BanPickConfirmRequest;
import com.example.demo.dto.banpick.BanPickLineupConfirmRequest;
import com.example.demo.dto.banpick.BanPickLineupReorderRequest;
import com.example.demo.dto.banpick.BanPickRoomStateResponse;
import com.example.demo.security.GoogleUserAuthenticationToken;
import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.BanPickRoomBroadcaster;
import com.example.demo.service.BanPickRoomService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@Controller
public class BanPickRoomWebSocketController {

    private final BanPickRoomService banPickRoomService;
    private final BanPickRoomBroadcaster banPickRoomBroadcaster;

    public BanPickRoomWebSocketController(BanPickRoomService banPickRoomService,
                                          BanPickRoomBroadcaster banPickRoomBroadcaster) {
        this.banPickRoomService = banPickRoomService;
        this.banPickRoomBroadcaster = banPickRoomBroadcaster;
    }

    @MessageMapping("/ban-pick/{roomCode}/confirm")
    public void confirm(@DestinationVariable String roomCode,
                        @Payload BanPickConfirmRequest request,
                        Principal principal) {
        broadcast(banPickRoomService.confirmAction(roomCode, request, currentUser(principal)));
    }

    @MessageMapping("/ban-pick/{roomCode}/lineup/reorder")
    public void reorderLineup(@DestinationVariable String roomCode,
                              @Payload BanPickLineupReorderRequest request,
                              Principal principal) {
        broadcast(banPickRoomService.reorderLineup(roomCode, request, currentUser(principal)));
    }

    @MessageMapping("/ban-pick/{roomCode}/lineup/confirm")
    public void confirmLineup(@DestinationVariable String roomCode,
                              @Payload(required = false) BanPickLineupConfirmRequest request,
                              Principal principal) {
        broadcast(banPickRoomService.confirmLineup(roomCode, request, currentUser(principal)));
    }

    @MessageMapping("/ban-pick/{roomCode}/start")
    public void start(@DestinationVariable String roomCode, Principal principal) {
        broadcast(banPickRoomService.startRoom(roomCode, currentUser(principal)));
    }

    @MessageMapping("/ban-pick/{roomCode}/roll-side")
    public void rollSide(@DestinationVariable String roomCode, Principal principal) {
        broadcast(banPickRoomService.rollSide(roomCode, currentUser(principal)));
    }

    @MessageMapping("/ban-pick/{roomCode}/ready")
    public void ready(@DestinationVariable String roomCode, Principal principal) {
        broadcast(banPickRoomService.readyRoom(roomCode, currentUser(principal)));
    }

    private void broadcast(BanPickRoomStateResponse roomState) {
        banPickRoomBroadcaster.broadcast(roomState);
    }

    private GoogleUserPrincipal currentUser(Principal principal) {
        if (principal instanceof GoogleUserAuthenticationToken token
                && token.getPrincipal() instanceof GoogleUserPrincipal googleUserPrincipal) {
            return googleUserPrincipal;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap");
    }

    @MessageExceptionHandler
    @SendToUser(value = "/queue/ban-pick/errors", broadcast = false)
    public Map<String, String> handleMessagingException(Exception exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            String message = responseStatusException.getReason() != null
                    ? responseStatusException.getReason()
                    : responseStatusException.getMessage();
            return Map.of("message", message);
        }
        if (hasCause(exception, DataIntegrityViolationException.class)) {
            return Map.of("message", "Tướng này đã được chọn hoặc cấm.");
        }
        return Map.of("message", "Trạng thái phòng đã thay đổi, vui lòng thử lại.");
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
