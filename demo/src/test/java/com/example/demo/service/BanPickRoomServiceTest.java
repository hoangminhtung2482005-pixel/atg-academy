package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickConfirmRequest;
import com.example.demo.dto.banpick.BanPickLineupConfirmRequest;
import com.example.demo.dto.banpick.BanPickLineupReorderRequest;
import com.example.demo.entity.BanPickAction;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickParticipantRole;
import com.example.demo.entity.BanPickPhaseType;
import com.example.demo.entity.BanPickRoom;
import com.example.demo.entity.BanPickRoomParticipant;
import com.example.demo.entity.BanPickRoomStatus;
import com.example.demo.entity.BanPickSeriesType;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.Hero;
import com.example.demo.entity.User;
import com.example.demo.repository.BanPickActionRepository;
import com.example.demo.repository.BanPickRoomParticipantRepository;
import com.example.demo.repository.BanPickRoomRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BanPickRoomServiceTest {

    private static final String ROOM_CODE = "ABC123";

    @Mock
    private BanPickRoomRepository roomRepository;

    @Mock
    private BanPickRoomParticipantRepository participantRepository;

    @Mock
    private BanPickActionRepository actionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HeroRepository heroRepository;

    @Mock
    private BanPickHistoryService banPickHistoryService;

    private BanPickRoomService service;

    @BeforeEach
    void setUp() {
        service = new BanPickRoomService(
                roomRepository,
                participantRepository,
                actionRepository,
                userRepository,
                heroRepository,
                banPickHistoryService
        );

        when(roomRepository.save(any(BanPickRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.save(any(BanPickRoomParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.saveAndFlush(any(BanPickAction.class))).thenAnswer(invocation -> {
            BanPickAction action = invocation.getArgument(0);
            if (action.getId() == null) {
                action.setId(100L);
            }
            return action;
        });
        when(participantRepository.findByRoomOrderByJoinedAtAsc(any(BanPickRoom.class))).thenReturn(List.of());
        when(actionRepository.findByRoomOrderByConfirmedAtAsc(any(BanPickRoom.class))).thenReturn(List.of());
        when(banPickHistoryService.findLatestHistoryIdByRoomCode(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void guestCanJoinWaitingRoom() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = waitingRoom(host);

        when(userRepository.findByEmail(guest.getEmail())).thenReturn(Optional.of(guest));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        service.joinRoom(ROOM_CODE, principal(guest));

        assertThat(room.getGuestUser()).isEqualTo(guest);
        assertThat(room.getStatus()).isEqualTo(BanPickRoomStatus.READY);

        ArgumentCaptor<BanPickRoomParticipant> participantCaptor =
                ArgumentCaptor.forClass(BanPickRoomParticipant.class);
        verify(participantRepository).save(participantCaptor.capture());
        assertThat(participantCaptor.getValue().getRole()).isEqualTo(BanPickParticipantRole.GUEST);
        assertThat(participantCaptor.getValue().getUser()).isEqualTo(guest);
    }

    @Test
    void thirdUserCannotJoinFullRoom() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        User third = user(3L, "third@example.com");
        BanPickRoom room = waitingRoom(host);
        room.setGuestUser(guest);
        room.setStatus(BanPickRoomStatus.READY);

        when(userRepository.findByEmail(third.getEmail())).thenReturn(Optional.of(third));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.joinRoom(ROOM_CODE, principal(third)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Phòng đã đủ người.");
                });
    }

    @Test
    void onlyHostCanRollSide() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = readyRoom(host, guest);

        when(userRepository.findByEmail(guest.getEmail())).thenReturn(Optional.of(guest));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.rollSide(ROOM_CODE, principal(guest)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo("Only host can perform this action");
                });
    }

    @Test
    void sideCanOnlyBeRolledOnce() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = readyRoom(host, guest);
        room.setBlueUser(host);
        room.setRedUser(guest);

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.rollSide(ROOM_CODE, principal(host)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Hai bên đã được phân định.");
                });
    }

    @Test
    void expiredBanPhaseAutoSkipsAndAdvances() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setPhaseDeadlineAt(LocalDateTime.now().minusSeconds(1));

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        service.confirmAction(ROOM_CODE, banRequest(1L), principal(host));

        assertThat(room.getCurrentPhaseIndex()).isEqualTo(1);
        assertThat(room.getCurrentPhaseSelectedCount()).isZero();
        verify(actionRepository, never()).saveAndFlush(any(BanPickAction.class));
    }

    @Test
    void expiredPickPhaseAutoPicksAvailableHero() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setCurrentPhaseIndex(4);
        room.setPhaseDeadlineAt(LocalDateTime.now().minusSeconds(1));

        Hero hero = hero(10L, "Aoi");
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.findAll()).thenReturn(List.of(hero));
        when(actionRepository.saveAndFlush(any(BanPickAction.class))).thenAnswer(invocation -> {
            BanPickAction action = invocation.getArgument(0);
            action.setId(100L);
            return action;
        });

        service.resolveExpiredPhase(ROOM_CODE);

        ArgumentCaptor<BanPickAction> actionCaptor = ArgumentCaptor.forClass(BanPickAction.class);
        verify(actionRepository).saveAndFlush(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(BanPickActionType.PICK);
        assertThat(actionCaptor.getValue().getHeroId()).isEqualTo(10L);
        assertThat(actionCaptor.getValue().getPhaseIndex()).isEqualTo(4);
        assertThat(room.getCurrentPhaseIndex()).isEqualTo(5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void finalPickStartsLineupAdjustmentInsteadOfFinishing() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setCurrentPhaseIndex(14);
        room.setCurrentPhaseSelectedCount(0);

        List<BanPickAction> draftActions = List.of(
                action(room, host, BanPickTeamSide.BLUE, BanPickActionType.PICK, 1L, 4),
                action(room, host, BanPickTeamSide.BLUE, BanPickActionType.PICK, 2L, 6),
                action(room, host, BanPickTeamSide.BLUE, BanPickActionType.PICK, 3L, 6),
                action(room, host, BanPickTeamSide.BLUE, BanPickActionType.PICK, 4L, 13),
                action(room, host, BanPickTeamSide.BLUE, BanPickActionType.PICK, 5L, 13),
                action(room, guest, BanPickTeamSide.RED, BanPickActionType.PICK, 6L, 5),
                action(room, guest, BanPickTeamSide.RED, BanPickActionType.PICK, 7L, 5),
                action(room, guest, BanPickTeamSide.RED, BanPickActionType.PICK, 8L, 7),
                action(room, guest, BanPickTeamSide.RED, BanPickActionType.PICK, 9L, 12),
                action(room, guest, BanPickTeamSide.RED, BanPickActionType.PICK, 10L, 14)
        );

        when(userRepository.findByEmail(guest.getEmail())).thenReturn(Optional.of(guest));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(10L)).thenReturn(true);
        when(actionRepository.findByRoomOrderByConfirmedAtAsc(room)).thenReturn(draftActions, draftActions, draftActions);

        service.confirmAction(ROOM_CODE, pickRequest(BanPickTeamSide.RED, 10L), principal(guest));

        assertThat(room.getStatus()).isEqualTo(BanPickRoomStatus.IN_PROGRESS);
        assertThat(room.getPhaseType()).isEqualTo(BanPickPhaseType.LINEUP_ADJUSTMENT);
        assertThat(room.getLineupDeadlineAt()).isNotNull();
        assertThat(room.getLineupDeadlineAt()).isAfter(room.getTimerStartedAt());
        assertThat(java.time.Duration.between(room.getTimerStartedAt(), room.getLineupDeadlineAt()).getSeconds()).isEqualTo(30);
        assertThat(room.getBluePickOrder()).isEqualTo("1,2,3,4,5");
        assertThat(room.getRedPickOrder()).isEqualTo("6,7,8,9,10");
    }

    @Test
    void blueCanReorderOnlyBluePicks() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = lineupRoom(host, guest);

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        service.reorderLineup(ROOM_CODE,
                new BanPickLineupReorderRequest(BanPickTeamSide.BLUE, List.of(2L, 1L, 3L, 4L, 5L)),
                principal(host));

        assertThat(room.getBluePickOrder()).isEqualTo("2,1,3,4,5");

        assertThatThrownBy(() -> service.reorderLineup(ROOM_CODE,
                new BanPickLineupReorderRequest(BanPickTeamSide.RED, List.of(10L, 9L, 8L, 7L, 6L)),
                principal(host)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void redCanReorderOnlyRedPicks() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = lineupRoom(host, guest);

        when(userRepository.findByEmail(guest.getEmail())).thenReturn(Optional.of(guest));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        service.reorderLineup(ROOM_CODE,
                new BanPickLineupReorderRequest(BanPickTeamSide.RED, List.of(7L, 6L, 8L, 9L, 10L)),
                principal(guest));

        assertThat(room.getRedPickOrder()).isEqualTo("7,6,8,9,10");
    }

    @Test
    void reorderCannotAddRemoveOrDuplicateHeroes() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = lineupRoom(host, guest);

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.reorderLineup(ROOM_CODE,
                new BanPickLineupReorderRequest(BanPickTeamSide.BLUE, List.of(1L, 2L, 3L, 4L)),
                principal(host)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> service.reorderLineup(ROOM_CODE,
                new BanPickLineupReorderRequest(BanPickTeamSide.BLUE, List.of(1L, 1L, 3L, 4L, 5L)),
                principal(host)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void bothLineupConfirmsEndDraft() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = lineupRoom(host, guest);
        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(userRepository.findByEmail(guest.getEmail())).thenReturn(Optional.of(guest));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        service.confirmLineup(ROOM_CODE, new BanPickLineupConfirmRequest(BanPickTeamSide.BLUE), principal(host));
        assertThat(room.getStatus()).isEqualTo(BanPickRoomStatus.IN_PROGRESS);
        assertThat(room.getBlueLineupConfirmed()).isTrue();

        service.confirmLineup(ROOM_CODE, new BanPickLineupConfirmRequest(BanPickTeamSide.RED), principal(guest));
        assertThat(room.getStatus()).isEqualTo(BanPickRoomStatus.FINISHED);
        assertThat(room.getRedLineupConfirmed()).isTrue();
    }

    @Test
    void lineupTimeoutEndsDraftAutomatically() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = lineupRoom(host, guest);
        room.setLineupDeadlineAt(LocalDateTime.now().minusSeconds(1));

        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        var result = service.resolveExpiredPhase(ROOM_CODE);

        assertThat(result).isPresent();
        assertThat(room.getStatus()).isEqualTo(BanPickRoomStatus.FINISHED);
    }

    @Test
    void startRequiresBothPlayersReady() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = readyRoom(host, guest);
        room.setBlueUser(host);
        room.setRedUser(guest);

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.startRoom(ROOM_CODE, principal(host)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Cả hai người chơi phải sẵn sàng.");
                });
    }

    @Test
    void readyRoomMarksCurrentParticipantReady() {
        User host = user(1L, "host@example.com");
        BanPickRoom room = waitingRoom(host);

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        service.readyRoom(ROOM_CODE, principal(host));

        assertThat(room.getHostReady()).isTrue();
        assertThat(room.getGuestReady()).isFalse();
    }

    @Test
    void duplicateHeroIsRejected() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(1L)).thenReturn(true);
        when(actionRepository.existsByRoomAndHeroId(room, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.confirmAction(ROOM_CODE, banRequest(1L), principal(host)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Tướng này đã được chọn hoặc cấm.");
                });
    }

    @Test
    void wrongPlayerCannotAct() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);

        when(userRepository.findByEmail(guest.getEmail())).thenReturn(Optional.of(guest));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.confirmAction(ROOM_CODE, banRequest(1L), principal(guest)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo("Chưa đến lượt của bạn.");
                });
    }

    @Test
    void bo3BlocksSameSideFromReusingPreviousPick() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setSeriesType(BanPickSeriesType.BO3);
        room.setCurrentGameNumber(2);
        room.setCurrentPhaseIndex(4);
        room.setBlueSeriesUsedHeroIds("10");
        room.setBlueUsedPicksByGame("1:10");

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.confirmAction(ROOM_CODE, pickRequest(BanPickTeamSide.BLUE, 10L), principal(host)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Tướng này đã được đội của bạn sử dụng ở ván trước.");
                });

        verify(actionRepository, never()).saveAndFlush(any(BanPickAction.class));
    }

    @Test
    void opponentCanPickHeroUsedByOtherSideInPreviousGame() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setSeriesType(BanPickSeriesType.BO3);
        room.setCurrentGameNumber(2);
        room.setCurrentPhaseIndex(5);
        room.setBlueSeriesUsedHeroIds("10");
        room.setBlueUsedPicksByGame("1:10");

        when(userRepository.findByEmail(guest.getEmail())).thenReturn(Optional.of(guest));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(10L)).thenReturn(true);

        service.confirmAction(ROOM_CODE, pickRequest(BanPickTeamSide.RED, 10L), principal(guest));

        ArgumentCaptor<BanPickAction> actionCaptor = ArgumentCaptor.forClass(BanPickAction.class);
        verify(actionRepository).saveAndFlush(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getTeamSide()).isEqualTo(BanPickTeamSide.RED);
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(BanPickActionType.PICK);
        assertThat(actionCaptor.getValue().getHeroId()).isEqualTo(10L);
    }

    @Test
    void previousGameBansDoNotRestrictNextGame() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setSeriesType(BanPickSeriesType.BO3);
        room.setCurrentGameNumber(2);
        room.setCurrentPhaseIndex(0);
        room.setBlueSeriesUsedHeroIds("10");
        room.setBlueUsedPicksByGame("1:10");

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(10L)).thenReturn(true);

        service.confirmAction(ROOM_CODE, banRequest(10L), principal(host));

        ArgumentCaptor<BanPickAction> actionCaptor = ArgumentCaptor.forClass(BanPickAction.class);
        verify(actionRepository).saveAndFlush(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(BanPickActionType.BAN);
        assertThat(actionCaptor.getValue().getHeroId()).isEqualTo(10L);
    }

    @Test
    void bo1DoesNotRestrictPreviousPickHistory() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setSeriesType(BanPickSeriesType.BO1);
        room.setCurrentGameNumber(1);
        room.setCurrentPhaseIndex(4);
        room.setBlueSeriesUsedHeroIds("10");

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(10L)).thenReturn(true);

        service.confirmAction(ROOM_CODE, pickRequest(BanPickTeamSide.BLUE, 10L), principal(host));

        verify(actionRepository).saveAndFlush(any(BanPickAction.class));
    }

    @Test
    void bo7GameSevenResetsPreviousPickRestriction() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setSeriesType(BanPickSeriesType.BO7);
        room.setCurrentGameNumber(7);
        room.setCurrentPhaseIndex(4);
        room.setBlueSeriesUsedHeroIds("10,11,12");

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(10L)).thenReturn(true);

        service.confirmAction(ROOM_CODE, pickRequest(BanPickTeamSide.BLUE, 10L), principal(host));

        verify(actionRepository).saveAndFlush(any(BanPickAction.class));
    }

    @Test
    void roomStateExposesUsedHeroesByTeamForActiveSeriesRestriction() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setSeriesType(BanPickSeriesType.BO5);
        room.setCurrentGameNumber(3);
        room.setBlueSeriesUsedHeroIds("10,11");
        room.setRedSeriesUsedHeroIds("20");
        room.setBlueUsedPicksByGame("1:10;2:11");
        room.setRedUsedPicksByGame("1:20");

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        var state = service.getRoomState(ROOM_CODE, principal(host));

        assertThat(state.usedHeroesByTeam()).containsEntry("blue", List.of(10L, 11L));
        assertThat(state.usedHeroesByTeam()).containsEntry("red", List.of(20L));
        assertThat(state.blueUsedPicks()).containsExactly(10L, 11L);
        assertThat(state.redUsedPicks()).containsExactly(20L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void bo7GameSevenClearsUsedHeroesByTeamOnNextGame() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setSeriesType(BanPickSeriesType.BO7);
        room.setCurrentGameNumber(6);
        room.setStatus(BanPickRoomStatus.FINISHED);
        room.setBlueSeriesUsedHeroIds("10,11,12");
        room.setRedSeriesUsedHeroIds("20,21,22");
        room.setBlueUsedPicksByGame("1:10;2:11;3:12");
        room.setRedUsedPicksByGame("1:20;2:21;3:22");

        List<BanPickAction> finishedActions = List.of(
                action(room, host, BanPickTeamSide.BLUE, BanPickActionType.PICK, 13L, 4),
                action(room, guest, BanPickTeamSide.RED, BanPickActionType.PICK, 23L, 5)
        );

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(actionRepository.findByRoomOrderByConfirmedAtAsc(room)).thenReturn(finishedActions, List.of());

        var state = service.nextGame(ROOM_CODE, principal(host));

        assertThat(room.getCurrentGameNumber()).isEqualTo(7);
        assertThat(room.getBlueSeriesUsedHeroIds()).isNull();
        assertThat(room.getRedSeriesUsedHeroIds()).isNull();
        assertThat(state.bo7ResetActive()).isTrue();
        assertThat(state.usedHeroesByTeam().get("blue")).isEmpty();
        assertThat(state.usedHeroesByTeam().get("red")).isEmpty();
        assertThat(state.blueUsedPicks()).isEmpty();
        assertThat(state.redUsedPicks()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void nextGameClearsCurrentDraftAndIncrementsGameNumber() {
        User host = user(1L, "host@example.com");
        User guest = user(2L, "guest@example.com");
        BanPickRoom room = startedRoom(host, guest);
        room.setSeriesType(BanPickSeriesType.BO3);
        room.setCurrentGameNumber(1);
        room.setStatus(BanPickRoomStatus.FINISHED);
        room.setTimerStartedAt(null);
        room.setPhaseDeadlineAt(null);

        List<BanPickAction> finishedActions = List.of(
                action(room, host, BanPickTeamSide.BLUE, BanPickActionType.BAN, 1L, 0),
                action(room, host, BanPickTeamSide.BLUE, BanPickActionType.PICK, 10L, 4),
                action(room, guest, BanPickTeamSide.RED, BanPickActionType.PICK, 20L, 5)
        );

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(actionRepository.findByRoomOrderByConfirmedAtAsc(room)).thenReturn(finishedActions, List.of());

        var state = service.nextGame(ROOM_CODE, principal(host));

        verify(actionRepository).deleteByRoom(room);
        assertThat(room.getStatus()).isEqualTo(BanPickRoomStatus.IN_PROGRESS);
        assertThat(room.getCurrentGameNumber()).isEqualTo(2);
        assertThat(room.getCurrentPhaseIndex()).isZero();
        assertThat(room.getCurrentPhaseSelectedCount()).isZero();
        assertThat(room.getTimerStartedAt()).isNotNull();
        assertThat(room.getPhaseDeadlineAt()).isNotNull();
        assertThat(room.getBlueUsedPicksByGame()).isEqualTo("1:10");
        assertThat(room.getRedUsedPicksByGame()).isEqualTo("1:20");
        assertThat(room.getBlueSeriesUsedHeroIds()).isEqualTo("10");
        assertThat(room.getRedSeriesUsedHeroIds()).isEqualTo("20");
        assertThat(state.currentGameNumber()).isEqualTo(2);
        assertThat(state.actions()).isEmpty();
    }

    private static BanPickConfirmRequest banRequest(Long heroId) {
        return new BanPickConfirmRequest(BanPickTeamSide.BLUE, BanPickActionType.BAN, heroId, null);
    }

    private static BanPickConfirmRequest pickRequest(BanPickTeamSide teamSide, Long heroId) {
        return new BanPickConfirmRequest(teamSide, BanPickActionType.PICK, heroId, null);
    }

    private static BanPickRoom waitingRoom(User host) {
        BanPickRoom room = new BanPickRoom();
        room.setId(10L);
        room.setRoomCode(ROOM_CODE);
        room.setHostUser(host);
        room.setStatus(BanPickRoomStatus.WAITING);
        room.setPhaseType(BanPickPhaseType.DRAFT);
        room.setCurrentPhaseIndex(0);
        room.setCurrentPhaseSelectedCount(0);
        room.setPhaseDurationSeconds(60);
        room.setSeriesType(BanPickSeriesType.BO1);
        room.setCurrentGameNumber(1);
        return room;
    }

    private static BanPickRoom readyRoom(User host, User guest) {
        BanPickRoom room = waitingRoom(host);
        room.setGuestUser(guest);
        room.setStatus(BanPickRoomStatus.READY);
        return room;
    }

    private static BanPickRoom startedRoom(User host, User guest) {
        BanPickRoom room = readyRoom(host, guest);
        room.setBlueUser(host);
        room.setRedUser(guest);
        room.setStatus(BanPickRoomStatus.IN_PROGRESS);
        room.setPhaseType(BanPickPhaseType.DRAFT);
        room.setTimerStartedAt(LocalDateTime.now());
        room.setPhaseDeadlineAt(LocalDateTime.now().plusSeconds(60));
        return room;
    }

    private static BanPickRoom lineupRoom(User host, User guest) {
        BanPickRoom room = startedRoom(host, guest);
        room.setPhaseType(BanPickPhaseType.LINEUP_ADJUSTMENT);
        room.setCurrentPhaseIndex(15);
        room.setCurrentPhaseSelectedCount(0);
        room.setTimerStartedAt(LocalDateTime.now());
        room.setPhaseDeadlineAt(null);
        room.setLineupDeadlineAt(LocalDateTime.now().plusSeconds(30));
        room.setBluePickOrder("1,2,3,4,5");
        room.setRedPickOrder("6,7,8,9,10");
        return room;
    }

    private static User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(email);
        user.setAvatarUrl("");
        user.setRole("User");
        return user;
    }

    private static Hero hero(Long id, String name) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setName(name);
        return hero;
    }

    private static BanPickAction action(BanPickRoom room,
                                        User user,
                                        BanPickTeamSide teamSide,
                                        BanPickActionType actionType,
                                        Long heroId,
                                        int phaseIndex) {
        BanPickAction action = new BanPickAction();
        action.setRoom(room);
        action.setUser(user);
        action.setTeamSide(teamSide);
        action.setActionType(actionType);
        action.setHeroId(heroId);
        action.setPhaseIndex(phaseIndex);
        action.setConfirmedAt(LocalDateTime.now());
        return action;
    }

    private static GoogleUserPrincipal principal(User user) {
        return new GoogleUserPrincipal(user.getEmail(), user.getName(), "", user.getRole());
    }
}
