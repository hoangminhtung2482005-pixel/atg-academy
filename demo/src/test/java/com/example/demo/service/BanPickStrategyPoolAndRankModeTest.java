package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickConfirmRequest;
import com.example.demo.dto.banpick.BanPickRoomStateResponse;
import com.example.demo.dto.banpick.BanPickStrategyPoolRequest;
import com.example.demo.entity.BanPickAction;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickMatchMode;
import com.example.demo.entity.BanPickParticipantRole;
import com.example.demo.entity.BanPickPhaseType;
import com.example.demo.entity.BanPickRoom;
import com.example.demo.entity.BanPickRoomParticipant;
import com.example.demo.entity.BanPickRoomStatus;
import com.example.demo.entity.BanPickSeriesType;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroRole;
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
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for Strategy Pool and Rank Mode draft rules.
 * Covers:
 * - Strategy pool stored per participant
 * - Reconnect/read room restores strategy pool
 * - Cannot add own locked hero to pool
 * - Can add opponent locked hero to pool
 * - Cannot pick own locked hero in Game 2-6
 * - Can pick opponent locked hero
 * - Game 7 has no ban phase (Ultimate Battle)
 * - Game 1 has no prep phase
 * - Game 2-6 transitions prep -> draft on expiry
 * - SIMULATION mode is not affected
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BanPickStrategyPoolAndRankModeTest {

    private static final String ROOM_CODE = "RANK01";

    @Mock private BanPickRoomRepository roomRepository;
    @Mock private BanPickRoomParticipantRepository participantRepository;
    @Mock private BanPickActionRepository actionRepository;
    @Mock private UserRepository userRepository;
    @Mock private HeroRepository heroRepository;
    @Mock private BanPickHistoryService banPickHistoryService;

    private BanPickRoomService service;

    @BeforeEach
    void setUp() {
        BanPickRankedRoomContextService rankedCtx =
                new BanPickRankedRoomContextService(heroRepository, new Random(0));
        service = new BanPickRoomService(
                roomRepository, participantRepository, actionRepository,
                userRepository, heroRepository, banPickHistoryService,
                rankedCtx, BanPickRatingSettingsSnapshot::defaults
        );
        when(roomRepository.save(any(BanPickRoom.class))).thenAnswer(i -> i.getArgument(0));
        when(participantRepository.save(any(BanPickRoomParticipant.class))).thenAnswer(i -> i.getArgument(0));
        when(participantRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(actionRepository.saveAndFlush(any(BanPickAction.class))).thenAnswer(i -> {
            BanPickAction a = i.getArgument(0);
            if (a.getId() == null) a.setId(100L);
            return a;
        });
        when(heroRepository.findAllByPrimaryRoleIsNotNullOrderByNameAsc()).thenReturn(rankedHeroPool());
        when(participantRepository.findByRoomOrderByJoinedAtAsc(any())).thenReturn(List.of());
        when(actionRepository.findByRoomOrderByConfirmedAtAsc(any())).thenReturn(List.of());
        when(banPickHistoryService.findLatestHistoryIdByRoomCode(anyString())).thenReturn(Optional.empty());
    }

    // ─── Strategy Pool Tests ───────────────────────────────────────────────────

    @Test
    void strategyPoolStoredPerParticipant() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = rankedReadyRoom(host, guest);
        room.setBlueUser(host);
        room.setRedUser(guest);

        BanPickRoomParticipant hostParticipant = participant(room, host, BanPickParticipantRole.HOST, BanPickTeamSide.BLUE);
        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomOrderByJoinedAtAsc(room)).thenReturn(List.of(hostParticipant));

        service.updateStrategyPool(ROOM_CODE, new BanPickStrategyPoolRequest(List.of(10L, 20L, 30L)), principal(host));

        ArgumentCaptor<BanPickRoomParticipant> captor = ArgumentCaptor.forClass(BanPickRoomParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getStrategyPool()).isEqualTo("10,20,30");
    }

    @Test
    void reconnectRestoresStrategyPool() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = rankedReadyRoom(host, guest);
        room.setBlueUser(host);
        room.setRedUser(guest);

        BanPickRoomParticipant hostParticipant = participant(room, host, BanPickParticipantRole.HOST, BanPickTeamSide.BLUE);
        hostParticipant.setStrategyPool("10,20,30");
        BanPickRoomParticipant guestParticipant = participant(room, guest, BanPickParticipantRole.GUEST, BanPickTeamSide.RED);

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomOrderByJoinedAtAsc(room))
                .thenReturn(List.of(hostParticipant, guestParticipant));

        BanPickRoomStateResponse state = service.getRoomState(ROOM_CODE, principal(host));

        // Current user (host) should see their own pool
        assertThat(state.myStrategyPool()).containsExactly(10L, 20L, 30L);
        // Opponent pool should not be visible in myStrategyPool
        assertThat(state.participants()).hasSize(2);
        // Host participant should have pool, guest should not (private)
        assertThat(state.participants().get(0).strategyPool()).containsExactly(10L, 20L, 30L);
        assertThat(state.participants().get(1).strategyPool()).isNull();
    }

    @Test
    void cannotAddOwnLockedHeroToPool() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = rankedReadyRoom(host, guest);
        room.setBlueUser(host);
        room.setRedUser(guest);
        room.setBluePreviousUsedHeroIds("10,11,12");

        BanPickRoomParticipant hostParticipant = participant(room, host, BanPickParticipantRole.HOST, BanPickTeamSide.BLUE);
        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomOrderByJoinedAtAsc(room)).thenReturn(List.of(hostParticipant));

        // Hero 10 is in blue's own locked list → should be rejected
        assertThatThrownBy(() -> service.updateStrategyPool(
                ROOM_CODE, new BanPickStrategyPoolRequest(List.of(10L, 20L)), principal(host)))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void canAddOpponentLockedHeroToPool() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = rankedReadyRoom(host, guest);
        room.setBlueUser(host);
        room.setRedUser(guest);
        room.setBluePreviousUsedHeroIds("10,11");
        room.setRedPreviousUsedHeroIds("20,21");

        BanPickRoomParticipant hostParticipant = participant(room, host, BanPickParticipantRole.HOST, BanPickTeamSide.BLUE);
        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomOrderByJoinedAtAsc(room)).thenReturn(List.of(hostParticipant));

        // Hero 20 is in RED's locked list, but host is BLUE → should be allowed
        service.updateStrategyPool(ROOM_CODE, new BanPickStrategyPoolRequest(List.of(20L, 30L)), principal(host));

        ArgumentCaptor<BanPickRoomParticipant> captor = ArgumentCaptor.forClass(BanPickRoomParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getStrategyPool()).isEqualTo("20,30");
    }

    // ─── Draft Rules Tests ────────────────────────────────────────────────────

    @Test
    void cannotPickOwnLockedHeroInRankedGame2to6() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = rankedStartedRoom(host, guest);
        room.setVirtualGameIndex(3);
        room.setBluePreviousUsedHeroIds("99");
        room.setCurrentPhaseIndex(4); // first pick phase (BLUE PICK)

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(99L)).thenReturn(true);

        assertThatThrownBy(() -> service.confirmAction(
                ROOM_CODE, pickRequest(BanPickTeamSide.BLUE, 99L), principal(host)))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void canPickOpponentLockedHeroInRankedGame2to6() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = rankedStartedRoom(host, guest);
        room.setVirtualGameIndex(3);
        room.setRedPreviousUsedHeroIds("99"); // RED's locked hero
        room.setBluePreviousUsedHeroIds("50");
        room.setCurrentPhaseIndex(4); // BLUE PICK

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(99L)).thenReturn(true);

        // BLUE picking hero 99 (which is RED's locked hero) should be allowed
        service.confirmAction(ROOM_CODE, pickRequest(BanPickTeamSide.BLUE, 99L), principal(host));

        verify(actionRepository).saveAndFlush(any(BanPickAction.class));
    }

    @Test
    void game7UltimateBattleHasNoBanPhase() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = rankedStartedRoom(host, guest);
        room.setVirtualGameIndex(7);
        room.setUltimateBattle(true);
        room.setCurrentPhaseIndex(0); // first phase of Ultimate Battle

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        // In Ultimate Battle, phase 0 is BLUE PICK (not BAN)
        // Trying to BAN should fail because phase expects PICK
        assertThatThrownBy(() -> service.confirmAction(
                ROOM_CODE,
                new BanPickConfirmRequest(BanPickTeamSide.BLUE, BanPickActionType.BAN, 1L, null),
                principal(host)))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void game7UltimateBattleFirstPhaseIsBlindPick() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = rankedStartedRoom(host, guest);
        room.setVirtualGameIndex(7);
        room.setUltimateBattle(true);
        room.setCurrentPhaseIndex(0);

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(1L)).thenReturn(true);

        // Phase 0 of Ultimate Battle is BLUE PICK
        service.confirmAction(ROOM_CODE, pickRequest(BanPickTeamSide.BLUE, 1L), principal(host));

        ArgumentCaptor<BanPickAction> captor = ArgumentCaptor.forClass(BanPickAction.class);
        verify(actionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo(BanPickActionType.PICK);
    }

    @Test
    void game1HasNoPrepPhase() {
        BanPickRankedRoomContextService ctx = new BanPickRankedRoomContextService(heroRepository, new Random(0));
        assertThat(ctx.resolvePrepDurationSeconds(1)).isEqualTo(0);
    }

    @Test
    void game7HasNoPrepPhase() {
        BanPickRankedRoomContextService ctx = new BanPickRankedRoomContextService(heroRepository, new Random(0));
        assertThat(ctx.resolvePrepDurationSeconds(7)).isEqualTo(0);
    }

    @Test
    void game2to6HaveCorrectPrepDurations() {
        BanPickRankedRoomContextService ctx = new BanPickRankedRoomContextService(heroRepository, new Random(0));
        assertThat(ctx.resolvePrepDurationSeconds(2)).isEqualTo(30);
        assertThat(ctx.resolvePrepDurationSeconds(3)).isEqualTo(35);
        assertThat(ctx.resolvePrepDurationSeconds(4)).isEqualTo(40);
        assertThat(ctx.resolvePrepDurationSeconds(5)).isEqualTo(45);
        assertThat(ctx.resolvePrepDurationSeconds(6)).isEqualTo(50);
    }

    @Test
    void prepPhaseExpiryTransitionsToDraftPhase() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = rankedStartedRoom(host, guest);
        room.setVirtualGameIndex(2);
        room.setPrepDurationSeconds(30);
        // Prep phase already ended
        room.setPrepPhaseStartAt(LocalDateTime.now().minusSeconds(35));
        room.setPrepPhaseEndAt(LocalDateTime.now().minusSeconds(5));
        room.setPhaseDeadlineAt(room.getPrepPhaseEndAt()); // deadline = prep end

        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));

        var result = service.resolveExpiredPhase(ROOM_CODE);

        assertThat(result).isPresent();
        // After prep expires, prep window should be cleared and draft timer started
        assertThat(room.getPrepPhaseEndAt()).isNull();
        assertThat(room.getPrepPhaseStartAt()).isNull();
        assertThat(room.getPhaseDeadlineAt()).isAfter(LocalDateTime.now().minusSeconds(1));
    }

    @Test
    void simulationModeNotAffectedByRankedRules() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = simulationStartedRoom(host, guest);
        room.setCurrentPhaseIndex(4); // BLUE PICK

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(heroRepository.existsById(99L)).thenReturn(true);

        // In SIMULATION mode, no previous-used restrictions apply
        service.confirmAction(ROOM_CODE, pickRequest(BanPickTeamSide.BLUE, 99L), principal(host));

        verify(actionRepository).saveAndFlush(any(BanPickAction.class));
    }

    @Test
    void strategyPoolNotAffectedInSimulationMode() {
        User host = user(1L, "host@test.com");
        User guest = user(2L, "guest@test.com");
        BanPickRoom room = simulationStartedRoom(host, guest);
        room.setBlueUser(host);
        room.setRedUser(guest);

        BanPickRoomParticipant hostParticipant = participant(room, host, BanPickParticipantRole.HOST, BanPickTeamSide.BLUE);
        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomOrderByJoinedAtAsc(room)).thenReturn(List.of(hostParticipant));

        // In SIMULATION mode, no locked heroes → any hero can be added to pool
        service.updateStrategyPool(ROOM_CODE, new BanPickStrategyPoolRequest(List.of(99L, 100L)), principal(host));

        ArgumentCaptor<BanPickRoomParticipant> captor = ArgumentCaptor.forClass(BanPickRoomParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getStrategyPool()).isEqualTo("99,100");
    }

    // ─── Helper Methods ───────────────────────────────────────────────────────

    private static BanPickConfirmRequest pickRequest(BanPickTeamSide side, Long heroId) {
        return new BanPickConfirmRequest(side, BanPickActionType.PICK, heroId, null);
    }

    private static User user(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setName(email);
        u.setAvatarUrl("");
        u.setRole("User");
        return u;
    }

    private static GoogleUserPrincipal principal(User user) {
        return new GoogleUserPrincipal(user.getEmail(), user.getName(), "", user.getRole());
    }

    private static BanPickRoomParticipant participant(BanPickRoom room, User user,
                                                       BanPickParticipantRole role, BanPickTeamSide side) {
        BanPickRoomParticipant p = new BanPickRoomParticipant();
        p.setId(user.getId());
        p.setRoom(room);
        p.setUser(user);
        p.setRole(role);
        p.setTeamSide(side);
        return p;
    }

    private static BanPickRoom rankedReadyRoom(User host, User guest) {
        BanPickRoom room = new BanPickRoom();
        room.setId(1L);
        room.setRoomCode(ROOM_CODE);
        room.setMode(BanPickMatchMode.RANKED);
        room.setHostUser(host);
        room.setGuestUser(guest);
        room.setStatus(BanPickRoomStatus.READY);
        room.setPhaseType(BanPickPhaseType.DRAFT);
        room.setSeriesType(BanPickSeriesType.BO1);
        room.setVirtualSeriesFormat(BanPickSeriesType.BO7);
        room.setCurrentPhaseIndex(0);
        room.setCurrentPhaseSelectedCount(0);
        room.setPhaseDurationSeconds(60);
        room.setCurrentGameNumber(1);
        room.setUltimateBattle(false);
        room.setPrepDurationSeconds(0);
        return room;
    }

    private static BanPickRoom rankedStartedRoom(User host, User guest) {
        BanPickRoom room = rankedReadyRoom(host, guest);
        room.setBlueUser(host);
        room.setRedUser(guest);
        room.setStatus(BanPickRoomStatus.IN_PROGRESS);
        room.setTimerStartedAt(LocalDateTime.now());
        room.setPhaseDeadlineAt(LocalDateTime.now().plusSeconds(60));
        return room;
    }

    private static BanPickRoom simulationStartedRoom(User host, User guest) {
        BanPickRoom room = new BanPickRoom();
        room.setId(2L);
        room.setRoomCode(ROOM_CODE);
        room.setMode(BanPickMatchMode.SIMULATION);
        room.setHostUser(host);
        room.setGuestUser(guest);
        room.setBlueUser(host);
        room.setRedUser(guest);
        room.setStatus(BanPickRoomStatus.IN_PROGRESS);
        room.setPhaseType(BanPickPhaseType.DRAFT);
        room.setSeriesType(BanPickSeriesType.BO1);
        room.setCurrentPhaseIndex(0);
        room.setCurrentPhaseSelectedCount(0);
        room.setPhaseDurationSeconds(60);
        room.setCurrentGameNumber(1);
        room.setUltimateBattle(false);
        room.setPrepDurationSeconds(0);
        room.setTimerStartedAt(LocalDateTime.now());
        room.setPhaseDeadlineAt(LocalDateTime.now().plusSeconds(60));
        return room;
    }

    private static List<Hero> rankedHeroPool() {
        java.util.List<Hero> heroes = new java.util.ArrayList<>();
        long id = 1L;
        for (String code : BanPickRankedRoomContextService.REQUIRED_ROLE_CODES) {
            for (int i = 1; i <= 12; i++) {
                Hero h = new Hero();
                h.setId(id++);
                h.setName(code + "-" + i);
                HeroRole role = new HeroRole();
                role.setCode(code);
                role.setName(code);
                h.setPrimaryRole(role);
                heroes.add(h);
            }
        }
        return heroes;
    }
}
