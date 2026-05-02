package com.example.demo.dto.banpick;

public record BanPickCreateRoomResponse(
        String roomCode,
        String shareableUrl,
        BanPickRoomStateResponse room
) {
}
