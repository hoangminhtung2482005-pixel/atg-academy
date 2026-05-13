package com.example.demo.controller;

import com.example.demo.service.BanPickRoomService;
import com.example.demo.service.BanPickRoomBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BanPickRoomControllerTest {

    @Test
    void createRoomRequiresAuthentication() {
        BanPickRoomController controller = new BanPickRoomController(
                mock(BanPickRoomService.class),
                mock(BanPickRoomBroadcaster.class)
        );

        assertThatThrownBy(() -> controller.createRoom(null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isNotBlank();
                });
    }
}
