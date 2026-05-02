package com.example.demo.controller;

import com.example.demo.dto.banpick.BanPickConfirmRequest;
import com.example.demo.dto.banpick.BanPickCreateRoomRequest;
import com.example.demo.dto.banpick.BanPickCreateRoomResponse;
import com.example.demo.dto.banpick.BanPickLineupConfirmRequest;
import com.example.demo.dto.banpick.BanPickLineupReorderRequest;
import com.example.demo.dto.banpick.BanPickRoomStateResponse;
import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.BanPickRoomBroadcaster;
import com.example.demo.service.BanPickRoomService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ban-pick/rooms")
public class BanPickRoomController {

    private final BanPickRoomService banPickRoomService;
    private final BanPickRoomBroadcaster banPickRoomBroadcaster;

    public BanPickRoomController(BanPickRoomService banPickRoomService,
                                 BanPickRoomBroadcaster banPickRoomBroadcaster) {
        this.banPickRoomService = banPickRoomService;
        this.banPickRoomBroadcaster = banPickRoomBroadcaster;
    }

    @PostMapping
    public ResponseEntity<BanPickCreateRoomResponse> createRoom(Authentication authentication,
                                                                @RequestBody(required = false) BanPickCreateRoomRequest createRequest,
                                                                HttpServletRequest request) {
        BanPickRoomStateResponse room = banPickRoomService.createRoom(currentUser(authentication), createRequest);
        banPickRoomBroadcaster.broadcast(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(new BanPickCreateRoomResponse(
                room.roomCode(),
                buildShareableUrl(request, room.roomCode()),
                room
        ));
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<BanPickRoomStateResponse> getRoom(@PathVariable String roomCode,
                                                            Authentication authentication) {
        return ResponseEntity.ok(banPickRoomService.getRoomState(roomCode, currentUser(authentication)));
    }

    @PostMapping("/{roomCode}/join")
    public ResponseEntity<BanPickRoomStateResponse> joinRoom(@PathVariable String roomCode,
                                                             Authentication authentication) {
        return ResponseEntity.ok(broadcast(banPickRoomService.joinRoom(roomCode, currentUser(authentication))));
    }

    @PostMapping("/{roomCode}/roll-side")
    public ResponseEntity<BanPickRoomStateResponse> rollSide(@PathVariable String roomCode,
                                                             Authentication authentication) {
        return ResponseEntity.ok(broadcast(banPickRoomService.rollSide(roomCode, currentUser(authentication))));
    }

    @PostMapping("/{roomCode}/ready")
    public ResponseEntity<BanPickRoomStateResponse> readyRoom(@PathVariable String roomCode,
                                                              Authentication authentication) {
        return ResponseEntity.ok(broadcast(banPickRoomService.readyRoom(roomCode, currentUser(authentication))));
    }

    @PostMapping("/{roomCode}/start")
    public ResponseEntity<BanPickRoomStateResponse> startRoom(@PathVariable String roomCode,
                                                              Authentication authentication) {
        return ResponseEntity.ok(broadcast(banPickRoomService.startRoom(roomCode, currentUser(authentication))));
    }

    @PostMapping("/{roomCode}/confirm")
    public ResponseEntity<BanPickRoomStateResponse> confirmAction(@PathVariable String roomCode,
                                                                  @RequestBody BanPickConfirmRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(broadcast(banPickRoomService.confirmAction(roomCode, request, currentUser(authentication))));
    }

    @PostMapping("/{roomCode}/lineup/reorder")
    public ResponseEntity<BanPickRoomStateResponse> reorderLineup(@PathVariable String roomCode,
                                                                  @RequestBody BanPickLineupReorderRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(broadcast(banPickRoomService.reorderLineup(roomCode, request, currentUser(authentication))));
    }

    @PostMapping("/{roomCode}/lineup/confirm")
    public ResponseEntity<BanPickRoomStateResponse> confirmLineup(@PathVariable String roomCode,
                                                                  @RequestBody(required = false) BanPickLineupConfirmRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(broadcast(banPickRoomService.confirmLineup(roomCode, request, currentUser(authentication))));
    }

    @PostMapping("/{roomCode}/next-game")
    public ResponseEntity<BanPickRoomStateResponse> nextGame(@PathVariable String roomCode,
                                                             Authentication authentication) {
        return ResponseEntity.ok(broadcast(banPickRoomService.nextGame(roomCode, currentUser(authentication))));
    }

    @PostMapping("/{roomCode}/reset")
    public ResponseEntity<BanPickRoomStateResponse> resetRoom(@PathVariable String roomCode,
                                                              Authentication authentication) {
        return ResponseEntity.ok(broadcast(banPickRoomService.resetRoom(roomCode, currentUser(authentication))));
    }

    private BanPickRoomStateResponse broadcast(BanPickRoomStateResponse roomState) {
        banPickRoomBroadcaster.broadcast(roomState);
        return roomState;
    }

    private GoogleUserPrincipal currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof GoogleUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap");
        }
        return principal;
    }

    private String buildShareableUrl(HttpServletRequest request, String roomCode) {
        String origin = request.getScheme() + "://" + request.getServerName();
        if (!isDefaultPort(request)) {
            origin += ":" + request.getServerPort();
        }
        return origin + "/html/ban-pick.html?room=" + roomCode;
    }

    private boolean isDefaultPort(HttpServletRequest request) {
        return ("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 443);
    }
}
